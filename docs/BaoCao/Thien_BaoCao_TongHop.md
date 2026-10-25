# Báo cáo phần việc của Thiện — Vendor / Shipper / Payment / Realtime

Tài liệu này tổng hợp lại toàn bộ phần Thiện làm trong 6 tuần theo bảng phân công,
kèm giải thích các quyết định thiết kế để nhóm đọc nhanh khi ghép code và khi bảo vệ.

- Nhánh làm việc: `feat/thien/vendor-shipper-payment-realtime`
- Nhánh đích khi review: `develop` (sau khi nhóm kiểm tra mới merge `develop` → `main`)
- Không sửa trực tiếp trên giao diện GitHub; toàn bộ commit đi từ máy local.

---

## 1. Tổng quan theo tuần

| Tuần | Nội dung | Commit | Phạm vi chính |
|------|----------|--------|----------------|
| 1 | Đăng ký / quản lý shop | `cef7dad` | `module/vendor/*`, `templates/shop/*`, `templates/vendor/shop-form.html` |
| 2 | CRUD sản phẩm + size + topping + upload Cloudinary | `8f3ee55` | `module/vendor/*Product*`, `module/vendor/*Topping*`, `module/vendor/upload/*` |
| 3 | Quản lý đơn hàng của shop (7 trạng thái) | `893a1f8` | `module/orderstatus/*`, `module/vendor/VendorOrder*` |
| 4 | VNPAY / MOMO sandbox + WebSocket realtime | `1c896c8` | `module/payment/*`, `module/realtime/*`, `static/js/order-realtime.js` |
| 5a | Báo cáo doanh thu shop | `61a4fc1` | `module/vendor/VendorRevenue*` |
| 5b | Module Shipper | `88a5803` | `module/shipper/*`, `templates/shipper/*` |
| 6 | Dữ liệu demo + báo cáo | (commit này) | `module/demo/DemoOrderSeeder`, tài liệu này |

Mỗi tuần một commit để nhóm xem được diff gọn, không có commit "một cục".

---

## 2. Tuần 1 — Đăng ký và quản lý shop

**Luồng nghiệp vụ**

1. Người dùng bất kỳ đã đăng nhập vào `/shop/register` để gửi đơn đăng ký shop
   (tên shop, mô tả, địa chỉ, số điện thoại, logo, banner).
2. Đơn đăng ký được lưu với trạng thái `PENDING`. Người gửi **vẫn giữ vai `USER`**,
   chưa thành chủ shop — tránh trường hợp tự nâng quyền.
3. Admin/Manager duyệt ở khu quản trị: shop chuyển `ACTIVE` và chủ shop được đổi vai
   thành `VENDOR` (hợp đồng bàn giao với phần Admin/Manager, xem mục 8).
4. `/shop/{slug}` là trang công khai xem thông tin shop.

**Chống lỗi bảo mật**

- `VendorShopService` có `requireOwnedShop(username)`: mọi thao tác sửa shop đều đi qua
  bước này trước khi ghi, nên không thể sửa shop của người khác bằng cách đổi id trên URL.
- DTO `ShopUpdateRequest` **không có** trường `owner`, `status`, `commissionRate` —
  người dùng không thể tự gửi lên để đổi chủ shop, tự duyệt shop hay tự đổi chiết khấu.
- Ảnh tải lên đi qua `ImageUploadValidator` (xem tuần 2).

---

## 3. Tuần 2 — Sản phẩm, size, topping, upload Cloudinary

**Chức năng**

- Danh sách sản phẩm của shop, tạo/sửa/xoá mềm (đặt `active = false` khi sản phẩm đã
  có trong đơn hàng để không làm hỏng lịch sử đơn).
- Mỗi sản phẩm có nhiều size (`ProductVariant`: tên + giá cộng thêm).
- Topping quản lý ở cấp shop, chọn được khi đặt hàng.
- Ảnh sản phẩm tải lên Cloudinary qua `CloudinaryService` có sẵn của nhóm.

**Kiểm tra ảnh tải lên (`ImageUploadValidator`)**

| Kiểm tra | Cách làm |
|----------|----------|
| Rỗng / quá lớn | Giới hạn dung lượng, báo lỗi rõ ràng |
| Định dạng | Chỉ nhận `image/jpeg`, `image/png`, `image/webp` |
| Giả mạo content-type | Đọc **magic bytes** ở đầu file, không tin `Content-Type` của trình duyệt |
| Tên file | Chỉ lấy phần mở rộng, không dùng tên người dùng đặt để tránh path traversal |

Lỗi được gói trong `InvalidImageException` và hiển thị lại trên form, không làm sập trang.

---

## 4. Tuần 3 — Quản lý đơn hàng của shop (7 trạng thái)

### 4.1 Bảng chuyển trạng thái

Bảy trạng thái của `OrderStatus`: `NEW`, `CONFIRMED`, `PICKED_UP`, `SHIPPING`, `DELIVERED`,
`CANCELLED`, `RETURNED_REFUNDED`.

```
NEW ──► CONFIRMED ──► PICKED_UP ──► SHIPPING ──► DELIVERED
 │           │                          │             │
 │           └──────► CANCELLED         └─────────────┴──► RETURNED_REFUNDED
 └──────────────────► CANCELLED
```

`CANCELLED` và `RETURNED_REFUNDED` là trạng thái cuối, không đi tiếp được.

### 4.2 Ai được làm gì

`OrderStatusTransitions` giữ một bảng "vai trò × bước chuyển" duy nhất:

| Vai trò | Được phép |
|---------|-----------|
| `VENDOR` (chủ shop) | Xác nhận đơn mới, huỷ đơn mới/đã xác nhận, đánh dấu đã lấy hàng, xử lý trả hàng – hoàn tiền |
| `SHIPPER` | Đã lấy hàng, bắt đầu giao, giao thành công, báo giao thất bại |
| `CUSTOMER` | Tự huỷ khi đơn còn `NEW` |
| `ADMIN` | Toàn bộ |

Chủ shop **không tự đánh dấu "Đã giao"** — đây là điểm quan trọng: nếu shop tự đánh dấu
đã giao thì tiền COD và doanh thu sẽ sai. Chỉ shipper (người thực giao) mới được làm bước này.

### 4.3 Một chỗ xử lý duy nhất: `OrderStatusService`

Mọi thay đổi trạng thái — dù từ trang Vendor hay trang Shipper — đều đi qua
`OrderStatusService.change(...)`, nên luật chỉ nằm ở một nơi:

1. Kiểm tra bước chuyển có hợp lệ với vai trò không (sai → `IllegalStateException`, hiển thị lỗi).
2. Ghi thêm dòng ghi chú có dấu thời gian vào `Order.note` (tối đa 500 ký tự, cắt phần cũ nhất).
3. Chạy tác dụng phụ theo trạng thái:
   - `DELIVERED` → đơn COD chuyển `paymentStatus = PAID`, ghi `Shipment.deliveredAt`.
   - `CANCELLED` / `RETURNED_REFUNDED` → nếu đã thanh toán thì chuyển `REFUNDED`.
4. Phát sự kiện realtime cho khách, chủ shop và shipper (tuần 4).

Nhờ vậy Vendor (tuần 3) và Shipper (tuần 5) dùng chung một luật, không lệch nhau.

---

## 5. Tuần 4 — Thanh toán VNPAY/MOMO và realtime

### 5.1 Strategy pattern

```
PaymentStrategy (interface)
 ├── CodPaymentStrategy        → trả khi nhận hàng, không gọi cổng
 ├── VnPayPaymentStrategy      → ký HMAC-SHA512, tạo URL sandbox
 └── MomoPaymentStrategy       → ký HMAC-SHA256, gọi API tạo giao dịch
PaymentStrategyRegistry        → chọn strategy theo PaymentMethod
PaymentService                 → nghiệp vụ dùng chung: mở phiên, xử lý callback
```

Thêm cổng mới (ZaloPay, chuyển khoản…) chỉ cần thêm một class `implements PaymentStrategy`
và đăng ký — không sửa `PaymentService` hay controller. Đó chính là lợi ích của Strategy.

### 5.2 Kiểm tra chữ ký callback (3 lớp)

1. **Có cấu hình hay không**: thiếu secret → trả `CHUA_CAU_HINH`, không xử lý gì.
2. **Chữ ký có đúng không**: tính lại HMAC trên đúng tập tham số mà cổng quy định
   (VNPAY: 12 trường `vnp_*`; MOMO: 13 trường của API callback), so sánh **constant-time**
   bằng `MessageDigest.isEqual` để không bị tấn công đo thời gian.
3. **Đơn và số tiền có khớp không**: tra đơn theo `vnp_TxnRef` / `orderId`, so số tiền
   callback với `order.total`. Lệch → từ chối.

Đáp án IPN: VNPAY trả `{"RspCode":"00"}` khi thành công, `97` sai chữ ký, `01` không tìm thấy
đơn, `04` sai số tiền. MOMO trả `204` khi nhận, `400` khi sai chữ ký.

### 5.3 Realtime bằng WebSocket/STOMP

| Kênh | Ai nhận |
|------|---------|
| `/topic/shop/{shopId}/orders` | Trang quản lý đơn của shop (đơn mới + mọi thay đổi) |
| `/queue/orders` | Khách hàng và chủ shop theo dõi đơn của mình |
| `/queue/deliveries` | Shipper: được phân công đơn, đơn đổi trạng thái |

Client (`static/js/order-realtime.js`) nối SockJS, hiện toast và tải lại bảng đơn — không
phải F5. Mọi lỗi gửi realtime đều được bắt và ghi log: **gửi thông báo lỗi không bao giờ
làm hỏng giao dịch đặt hàng / đổi trạng thái**.

Cấu hình cổng thanh toán: xem [`Thien_CauHinh_VNPAY_MOMO.md`](Thien_CauHinh_VNPAY_MOMO.md).

---

## 6. Tuần 5 — Doanh thu và Shipper

### 6.1 Báo cáo doanh thu (`/vendor/revenue`)

- Doanh thu = tổng `total` của các đơn **đã giao** trong khoảng ngày (đơn huỷ / trả hàng không tính).
- Chiết khấu = doanh thu × `commissionRate` của shop / 100.
- Thực nhận = doanh thu − chiết khấu.
- Biểu đồ cột theo ngày hoặc theo tháng; các mốc không có đơn vẫn hiện cột 0 để biểu đồ liền mạch.
- Khoảng ngày bị giới hạn 366 ngày để không nặng máy; nếu người dùng chọn ngược thì tự đảo lại.
- Xuất được CSV (có BOM để Excel đọc đúng tiếng Việt).

### 6.2 Module Shipper

| Trang | Việc |
|-------|------|
| `/shipper` | Số đơn đang giao / đã giao / thất bại / đơn chờ nhận + thống kê nhanh tháng này |
| `/shipper/available` | Đơn shop đã xác nhận mà chưa ai nhận; bấm "Nhận đơn" để nhận |
| `/shipper/orders` | Đơn của tôi, lọc theo trạng thái, thao tác nhanh ngay trên bảng |
| `/shipper/orders/{id}` | Chi tiết: món cần giao, số điện thoại bấm gọi, địa chỉ kèm link bản đồ, điểm lấy hàng, tiền thu hộ |
| `/shipper/stats` | Thống kê theo ngày/tháng: đơn đã giao, thất bại, đang giao, tiền COD đã thu, phí giao hàng |

**Nhận đơn**: chỉ nhận được đơn `CONFIRMED` chưa có shipper; nhận xong hệ thống gán
`Order.shipper` và tạo `Shipment` (mã vận đơn, phí giao, mốc nhận đơn).

**Giao thất bại**: bắt buộc ghi lý do (tối đa 500 ký tự). Lý do được lưu vào
`Shipment.failedReason` và ghi thêm một dòng vào ghi chú đơn để chủ shop thấy ngay trong
trang quản lý đơn của mình.

**Tiền thu hộ (COD)**: chỉ đơn `COD` chưa thanh toán mới phải thu; đơn đã trả trước hiện
"đã trả trước" và shipper không thu tiền.

---

## 7. Tuần 6 — Dữ liệu demo

`DemoOrderSeeder` tạo ~29 đơn hàng demo rải đều 30 ngày gần nhất cho cả hai shop, đủ mọi
trạng thái, có món hàng, bản ghi giao hàng và bản ghi thanh toán để mọi màn hình có dữ liệu
thật mà xem. Bật bằng cấu hình riêng, mặc định tắt để không ảnh hưởng tới test tự động:

```properties
app.seed.enabled=true          # dữ liệu nền dùng chung (người dùng, shop, sản phẩm)
app.demo.orders.enabled=true   # đơn hàng demo phần của Thiện
```

Đặt cấu hình vào `application-local.properties` (đã có trong `.gitignore`), không sửa
`application.properties` dùng chung. Chạy lại nhiều lần không sinh đơn trùng.

---

## 8. Hợp đồng với phần Admin/Manager (cần nhóm xác nhận)

Để đơn đăng ký shop hoạt động trọn vẹn, phần Admin/Manager cần bổ sung thao tác duyệt:

1. Đọc danh sách shop `PENDING`.
2. Khi duyệt: đặt `Shop.status = ACTIVE` **và** đổi `User.role` của chủ shop thành `VENDOR`.
3. Khi từ chối: đặt `Shop.status = REJECTED` và ghi lý do.

Nếu bước 2 không làm, chủ shop đã được duyệt vẫn bị chặn khỏi `/vendor/**` vì `SecurityConfig`
yêu cầu vai `VENDOR`.

---

## 9. Checklist kiểm tra an toàn

| Hạng mục | Cách kiểm chứng | Kết quả |
|----------|-----------------|---------|
| Ownership / IDOR (vendor) | `VendorShopServiceTest`, `VendorProductServiceTest`, `VendorOrderServiceTest`, `VendorSecurityWebTest` | Đạt |
| Ownership / IDOR (shipper) | `ShipperOrderServiceTest#shipperKhacKhongChamDuocDon`, `ShipperOrderWebTest#shipperKhacKhongXemDuocDon` | Đạt |
| Chuyển trạng thái hợp lệ | `OrderStatusTransitionsTest`, `OrderStatusServiceTest` | Đạt — sai bước bị từ chối |
| Chữ ký callback | `PaymentSignaturesTest`, `VnPayPaymentStrategyTest`, `MomoPaymentStrategyTest`, `PaymentServiceTest` | Đạt — sai chữ ký / sai số tiền bị từ chối |
| Không lộ secret | Secret đọc từ cấu hình với giá trị mặc định rỗng; `application-local.properties` nằm trong `.gitignore` | Đạt |
| Ảnh tải lên hợp lệ | `ImageUploadValidatorTest` | Đạt — chỉ nhận jpeg/png/webp đúng magic bytes |

Tổng số test tự động của cả dự án sau khi ghép phần Thiện: 170 test, chạy bằng `mvn test`.

---

## 10. Điểm cần nhóm biết

1. **Ba file dùng chung có thêm truy vấn** (`OrderRepository`, `OrderItemRepository`,
   `ProductRepository`, `ProductVariantRepository`, `ToppingRepository`,
   `CartItemRepository`): chỉ **thêm** phương thức mới ở cuối file, đánh dấu bằng comment
   `// ===== Them cho module vendor / shipper (Thien) =====`, không sửa phương thức cũ.
2. **Không sửa** `SecurityConfig`, `WebSocketConfig`, `application.properties`,
   entity, layout/fragments. Cấu hình bảo mật riêng cho cổng thanh toán nằm trong
   `PaymentSecurityConfig` với `@Order(1)` và `securityMatcher("/payment/**")`.
3. **Ghi chú trạng thái đang lưu vào `Order.note`** vì dự án chưa có bảng lịch sử trạng thái.
   Về lâu dài nên thêm bảng `order_status_history` để lưu vết đầy đủ; hiện tại ghi chú bị
   giới hạn 500 ký tự.
4. **Rủi ro bảo mật cần nhóm xử lý** (ngoài phạm vi của Thiện):
   - `WebSocketConfig` đang cho phép origin `*` → nên giới hạn theo domain thật.
   - `app.jwt.secret` trong `application.properties` là giá trị demo → phải đổi khi chạy thật.
5. **Sandbox VNPAY/MOMO chưa được kiểm chứng với cổng thật** vì nhóm chưa có
   `TmnCode`/`HashSecret`/`partnerCode`/`accessKey`. Phần chữ ký đã có test đối chiếu với
   tài liệu; khi có key chỉ cần điền cấu hình và chạy thử một giao dịch.
