# Hướng dẫn cấu hình cổng thanh toán VNPAY / MOMO (sandbox)

> Người làm: Thiện — Tuần 4. Tài liệu này thuộc module `payment`.
> Trạng thái: cấu hình và luồng chữ ký đã có **test tự động** trong
> `src/test/java/com/utetra/module/payment/`. Phần gọi **sandbox thật** chưa được
> kiểm chứng vì nhóm chưa có tài khoản merchant (xem mục 5).

## 1. Vì sao không đặt khoá trong `application.properties`

`application.properties` là file dùng chung và được đẩy lên GitHub. Khoá bí mật của
VNPAY/MOMO (hash secret, secret key) nằm trong danh sách **cấm commit** của nhóm.

Vì vậy module payment đọc cấu hình với giá trị mặc định rỗng:

```java
@ConfigurationProperties(prefix = "app.payment")
```

Ai cần chạy thanh toán thật thì tự thêm vào `application-local.properties`
(file này đã có trong `.gitignore`, không bị đẩy lên):

```properties
# VNPAY sandbox
app.payment.vnpay.tmn-code=MA_TMN_CUA_BAN
app.payment.vnpay.hash-secret=CHUOI_BI_MAT_CUA_BAN
app.payment.vnpay.return-url=http://localhost:8080/payment/vnpay/return
app.payment.vnpay.ipn-url=http://localhost:8080/payment/vnpay/ipn

# MOMO sandbox
app.payment.momo.partner-code=MA_PARTNER
app.payment.momo.access-key=ACCESS_KEY
app.payment.momo.secret-key=SECRET_KEY
app.payment.momo.return-url=http://localhost:8080/payment/momo/return
app.payment.momo.ipn-url=http://localhost:8080/payment/momo/ipn
```

Chưa điền khoá thì ứng dụng **vẫn chạy bình thường**:

- COD dùng được ngay.
- Chọn VNPAY/MOMO sẽ hiện thông báo "chưa được cấu hình" thay vì lỗi khó hiểu.

## 2. Lấy khoá sandbox ở đâu

| Cổng | Đăng ký | Khoá cần điền |
|---|---|---|
| VNPAY | https://sandbox.vnpayment.vn/devreg/ (tài khoản merchant sandbox) | `vnp_TmnCode`, `vnp_HashSecret` |
| MOMO | https://developers.momo.vn (tạo app ở môi trường test) | `partnerCode`, `accessKey`, `secretKey` |

Thông tin công khai (URL cổng, version, request type) đã được đặt sẵn trong
`PaymentGatewayProperties` nên không cần điền lại:

- VNPAY: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`, version `2.1.0`
- MOMO: `https://test-payment.momo.vn/v2/gateway/api/create`, `requestType=captureWallet`

## 3. Các đường dẫn module payment cung cấp

| Đường dẫn | Ai gọi | Việc làm |
|---|---|---|
| `GET  /payment/checkout/{orderId}` | khách (chủ đơn) | chọn phương thức, xem số tiền |
| `POST /payment/start/{orderId}` | khách (chủ đơn) | tạo giao dịch, chuyển sang cổng |
| `GET  /payment/result?ref=MA_DON` | khách | xem kết quả sau khi cổng trả về |
| `GET  /payment/vnpay/return` | VNPAY (trình duyệt khách) | kiểm tra chữ ký, cập nhật, đưa khách về trang kết quả |
| `GET  /payment/vnpay/ipn` | VNPAY (server gọi server) | cập nhật trạng thái, trả `RspCode` |
| `GET  /payment/momo/return` | MOMO (trình duyệt khách) | kiểm tra chữ ký, cập nhật |
| `POST /payment/momo/ipn` | MOMO (server gọi server) | cập nhật trạng thái, trả 204 |

Màn hình đặt hàng của Hải chỉ cần `redirect:/payment/checkout/{orderId}` sau khi lưu đơn.

`/payment/**` nằm trong **chuỗi lọc riêng** (`PaymentSecurityConfig`, `@Order(1)`) nên
không phải sửa `SecurityConfig` dùng chung: 4 đường callback được `permitAll`
(cổng không có phiên đăng nhập) và tắt CSRF cho riêng 2 đường IPN. Bảo vệ các đường
này bằng **chữ ký HMAC**, không bằng CSRF.

## 4. Ba chốt an toàn đã có test

1. **Chỉ chủ đơn được thanh toán đơn của mình** — `requireOwnOrder`, test IDOR ở
   `PaymentServiceTest.khongThanhToanHoDonNguoiKhac` và `PaymentWebTest`.
2. **Chữ ký sai thì không đổi gì** — `verifySignature` + so sánh không lộ thời gian
   (`MessageDigest.isEqual`). Test: `callbackSaiChuKyKhongDoiGi`.
3. **Số tiền callback phải khớp tổng tiền đơn** — chống sửa `vnp_Amount`/`amount`.
   Test: `callbackSaiSoTienKhongCapNhat`.

Ngoài ra cổng trả về mã lỗi (ví dụ VNPAY `vnp_ResponseCode=24`, MOMO `resultCode=1006`)
thì đơn chuyển `paymentStatus=FAILED` và khách được phép thử lại.

## 5. Điều CHƯA kiểm chứng (cần nhóm xác nhận khi có khoá thật)

Đây là phần trung thực cần ghi rõ trong báo cáo:

- **Chưa gọi sandbox thật lần nào** vì chưa có tài khoản merchant.
- Quy tắc mã hoá chuỗi ký của VNPAY dễ sai lệch giữa bên gửi và bên nhận
  (VNPAY giải mã rồi mã hoá lại theo cách của họ). Code hiện tại dùng RFC 3986
  (`%20` cho khoảng trắng) cho cả lúc tạo URL và lúc kiểm tra.
- MOMO: chuỗi ký lúc tạo giao dịch gồm đúng 10 tham số, chuỗi ký callback gồm đúng
  13 tham số (danh sách trong `MomoPaymentStrategy`). Nếu MOMO trả thêm tham số khác
  thì chữ ký vẫn đúng vì chỉ lấy đúng các tham số trong danh sách.
- IPN cần URL công khai (HTTPS) để cổng gọi vào. Khi demo bằng máy cá nhân phải dùng
  công cụ như ngrok và cập nhật lại `app.payment.*.ipn-url`.
- Chưa có bảng đối soát/hoàn tiền tự động: khi đơn bị huỷ sau khi đã trả tiền, hệ
  thống chỉ đánh dấu `paymentStatus=REFUNDED`, việc hoàn tiền thật vẫn làm trên
  trang quản trị của cổng.

## 6. Việc cần nhóm quyết

- Có lấy tài khoản sandbox VNPAY/MOMO để chạy thử thật không (mất khoảng 15 phút đăng ký).
- Có cần thêm bảng lịch sử trạng thái đơn (`order_status_history`) không: hiện tại lý do
  huỷ/trả hàng được ghi thêm vào `orders.note` để không phải sửa entity dùng chung.