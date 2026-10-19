# UTETra — Phân công nhiệm vụ & Cấu trúc dự án

**Đề tài 11:** Website chuỗi bán trà sữa UTETra — Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server/MySQL/PostgreSQL + JWT + WebSocket + Cloudinary.

**Nhóm 3 thành viên:** Quân, Hải, Thiện.
**Repo chung:** https://github.com/MinhQuan2309/WEB_PROJECT_TEAM17_11

---

## 1. Nguyên tắc chia việc

Đề tài yêu cầu *"phân công theo các chức năng theo User+Guest; Vendor, Manager, Admin, Shipper"*. Nhóm 3 người, 5 vai → gộp thành 3 khối cân nhau, mỗi khối có đủ cả **backend + frontend + báo cáo** để ai cũng bảo vệ được phần mình:

| Thành viên | Khối phụ trách | Vai trò trong đề tài |
|---|---|---|
| **Quân** | Nền tảng chung + Quản trị | Chức năng chung (auth/OTP/bảo mật/tìm kiếm) + **Manager** + **Admin** |
| **Hải** | Luồng mua hàng | **Guest** + **User** |
| **Thiện** | Người bán + vận chuyển + thanh toán | **Vendor** + **Shipper** |

Lý do chia vậy:
- Khối **chung** (đăng ký OTP email, Spring Security, mã hóa mật khẩu, upload Cloudinary, tìm kiếm/lọc) là thứ cả nhóm dùng → giao cho **một người làm xong sớm nhất**, kèm luôn Admin/Manager vì 2 vai này trong đề tài gần như trùng nhau (cùng một danh sách chức năng) nên làm chung tiết kiệm được ~40% công.
- Khối **User** nhiều màn nhất (giỏ hàng, thanh toán, lịch sử đơn, đánh giá, yêu thích, đã xem, voucher) → một người gánh trọn để luồng mua hàng liền mạch.
- Khối **Vendor** có cả quản lý sản phẩm + đơn + khuyến mãi + doanh thu; cộng thêm Shipper (nhỏ) và cổng thanh toán VNPAY/MOMO + WebSocket đẩy trạng thái đơn — hai phần "ăn điểm" khi demo.

---

## 2. Phân công chi tiết

### 2.1 Quân — Nền tảng chung + Manager + Admin

**A. Chức năng chung**

| Việc | Ghi chú kỹ thuật |
|---|---|
| ERD + toàn bộ entity & repository dùng chung | `User`, `Shop`, `Category`, `Product`, `ProductVariant`, `Topping`, `Cart`, `CartItem`, `Order`, `OrderItem`, `Address`, `Review`, `Voucher`, `Carrier`, `Shipment` |
| Đăng ký tài khoản + **gửi OTP qua Email** kích hoạt | `spring-boot-starter-mail`, bảng `OtpToken` (code, type, expiredAt, used), OTP 6 số hết hạn 5 phút |
| Đăng nhập / đăng xuất | Spring Security form login cho web + `/api/auth/login` trả JWT cho phần API/WebSocket |
| **Quên mật khẩu** + OTP email + đặt lại mật khẩu | cùng bảng `OtpToken`, type = `RESET_PASSWORD` |
| Mã hóa mật khẩu | `BCryptPasswordEncoder` — **không** lưu plain text ở bất kỳ đâu |
| Phân quyền 5 vai | `SecurityConfig`: `/admin/**` → ADMIN, `/manager/**` → MANAGER, `/vendor/**` → VENDOR, `/shipper/**` → SHIPPER, `/account/**` → đã đăng nhập |
| **Tìm kiếm & lọc sản phẩm** (dùng chung toàn hệ thống) | JPA `Specification` hoặc `@Query`: theo tên, danh mục, khoảng giá, shop, sắp xếp; trả `Page<T>` |
| Service upload ảnh/video Cloudinary | `CloudinaryService.upload(MultipartFile) → url`; Hải (ảnh bình luận) và Thiện (ảnh sản phẩm) gọi lại service này |
| Layout Bootstrap gốc + fragment chung | `layout/base.html` (Decorator), navbar theo vai, footer, phân trang, thông báo |
| Cấu hình CSDL + dữ liệu mẫu (seed) | `data.sql` hoặc `CommandLineRunner`: 1 admin, 1 manager, 2 vendor + 2 shop, 2 user, 1 shipper, ~15 sản phẩm |

**B. Manager + Admin**

Đề tài ghi Manager và Admin cùng một danh sách chức năng → làm **một bộ màn hình quản trị dùng chung**, khác nhau ở phạm vi dữ liệu:
- **Admin**: toàn sàn.
- **Manager**: chỉ các shop mình được phân công quản lý.

| Màn hình | Nội dung |
|---|---|
| Dashboard quản trị | số user, số shop, số đơn, doanh thu sàn, biểu đồ đơn theo ngày |
| Quản lý user | tìm kiếm, xem, khóa/mở khóa, đổi vai trò |
| Quản lý sản phẩm theo từng shop | duyệt / ẩn sản phẩm vi phạm |
| Quản lý danh mục | CRUD `Category` (có danh mục cha–con) |
| Quản lý chiết khấu app cho shop | % hoa hồng sàn thu trên mỗi đơn, theo shop |
| Quản lý chương trình khuyến mãi sàn | giảm % sản phẩm, giảm/miễn phí vận chuyển |
| Quản lý nhà vận chuyển | CRUD `Carrier` (tên, phí vận chuyển) |

**C. Báo cáo:** phần *Phân tích & thiết kế CSDL (ERD)*, *Bảo mật (Spring Security, BCrypt, JWT, OTP)*, *vai Manager & Admin*.

---

### 2.2 Hải — Guest + User (luồng mua hàng)

| Màn hình / chức năng | Yêu cầu từ đề tài |
|---|---|
| **Guest — Trang chủ** | hiển thị **trên 10 sản phẩm** của các shop, **sắp xếp giảm dần** (theo lượt bán); có ô tìm kiếm; bấm mua → chuyển sang đăng nhập |
| **User — Trang chủ** | 4 block **20 sản phẩm**: mới nhất / bán chạy nhất / đánh giá cao nhất / yêu thích nhiều nhất — **có phân trang hoặc lazy loading** |
| Trang sản phẩm theo danh mục | lọc theo danh mục + giá + shop, sắp xếp, phân trang (dùng service tìm kiếm của Quân) |
| Trang chi tiết sản phẩm | ảnh, mô tả, chọn size/topping/đường/đá, giá cập nhật theo tùy chọn, điểm đánh giá, danh sách bình luận, sản phẩm liên quan |
| Trang profile user | thông tin cá nhân, đổi mật khẩu, **quản lý nhiều địa chỉ nhận hàng** (CRUD + đặt mặc định) |
| **Giỏ hàng lưu trên database** | bảng `Cart` / `CartItem` theo user (không dùng session), thêm/sửa số lượng/xóa, nhóm theo shop |
| **Chọn mã giảm giá** | áp `Voucher` vào đơn, kiểm tra điều kiện (giá trị tối thiểu, hạn dùng, shop áp dụng) |
| Thanh toán | chọn địa chỉ + nhà vận chuyển + phương thức; **COD** do Hải làm, VNPAY/MOMO gọi sang module của Thiện |
| Lịch sử mua hàng theo trạng thái | tab: đơn mới / đã xác nhận / đang giao / đã giao / hủy / trả hàng–hoàn tiền; xem chi tiết đơn; nút hủy đơn, yêu cầu trả hàng |
| Thích sản phẩm | `Wishlist`, trang "sản phẩm yêu thích" |
| Sản phẩm đã xem | lưu lịch sử xem, hiển thị ở trang chủ |
| **Đánh giá** sản phẩm đã mua | 1–5 sao, chỉ cho phép khi đơn đã ở trạng thái *đã giao* |
| **Bình luận** sản phẩm đã mua | text **tối thiểu 50 ký tự** (validate cả client + server), kèm **ảnh/video** upload qua `CloudinaryService` |

**Báo cáo:** phần *vai Guest & User*, *luồng nghiệp vụ mua hàng*, *thiết kế giao diện người dùng*.

---

### 2.3 Thiện — Vendor + Shipper + Thanh toán + Real-time

**A. Vendor (Seller)** — có toàn bộ quyền của User, cộng thêm:

| Màn hình / chức năng | Nội dung |
|---|---|
| Đăng ký shop | form đăng ký (tên shop, mô tả, logo, địa chỉ), trạng thái `PENDING` → Admin/Manager duyệt → `ACTIVE`, user được nâng vai lên `VENDOR` |
| Quản lý trang chủ shop | sửa banner/logo/mô tả/giờ mở cửa; trang công khai `/shop/{id}` cho khách xem |
| Quản lý sản phẩm của shop | CRUD sản phẩm + size + topping + tồn kho, upload ảnh qua `CloudinaryService`, bật/tắt bán |
| Quản lý đơn hàng của shop theo trạng thái | **7 trạng thái**: đơn mới → đã xác nhận → đã lấy hàng → đang giao → đã giao / hủy / trả hàng–hoàn tiền; nút chuyển trạng thái |
| Tạo chương trình khuyến mãi | giảm % hoặc giảm tiền theo sản phẩm/toàn shop, thời gian hiệu lực, phát hành `Voucher` của shop |
| Quản lý doanh thu của shop | doanh thu theo ngày/tháng, trừ chiết khấu app, top sản phẩm bán chạy, biểu đồ |

**B. Shipper (phần thêm)**

| Màn hình | Nội dung |
|---|---|
| Đơn được phân công giao | danh sách đơn gán cho shipper, nhận đơn, cập nhật *đã lấy hàng → đang giao → đã giao*, lý do giao thất bại |
| Thống kê đơn | số đơn đã giao / đang giao / thất bại theo ngày–tháng, tổng tiền COD đã thu |

**C. Thanh toán (dùng chung, Hải gọi vào)**

- Áp dụng **Strategy pattern**: `PaymentStrategy` với 3 implement `CodPayment`, `VnPayPayment`, `MomoPayment`.
- Tích hợp VNPAY **hoặc** MOMO ở môi trường sandbox: tạo URL thanh toán, xử lý callback/IPN, cập nhật trạng thái đơn.

**D. Real-time WebSocket (điểm nhấn demo)**

- Khi Vendor hoặc Shipper đổi trạng thái đơn → đẩy sự kiện STOMP tới riêng user chủ đơn (`/user/queue/orders`) → màn "lịch sử đơn" của Hải cập nhật ngay, không cần F5.
- Thông báo cho Vendor khi có đơn mới.

**Báo cáo:** phần *vai Vendor & Shipper*, *tích hợp thanh toán*, *WebSocket real-time*, *design pattern*.

---

## 3. Cân đối khối lượng

| | Quân | Hải | Thiện |
|---|---|---|---|
| Số màn hình chính | ~13 | ~12 | ~11 |
| Entity làm chủ | 8 | 5 | 6 |
| Phần "khó" | Security + OTP + tìm kiếm | Giỏ hàng + checkout + đánh giá | Thanh toán + WebSocket |
| Phụ thuộc người khác | thấp (làm nền) | cao (chờ Quân tuần 1) | trung bình |

Ai xong sớm thì nhận thêm từ danh sách việc tùy chọn ở mục 8.

---

## 4. Cấu trúc dự án

Chia package **theo module nghiệp vụ** thay vì chỉ theo tầng. Mục đích rất thực tế: **mỗi người làm trong thư mục riêng của mình → gần như không bao giờ conflict khi merge Git**.

```
WEB_PROJECT_TEAM17_11/
├── docs/
│   ├── PHAN_CONG_NHIEM_VU.md          ← file này
│   ├── KhaoSat_PhanTichHeThong.docx
│   ├── ERD.png
│   └── BaoCao/                         (báo cáo từng thành viên)
├── pom.xml                             ⚠ file dùng chung — sửa phải báo nhóm
├── README.md
└── src/main/
    ├── java/com/utetra/
    │   ├── UtetraApplication.java
    │   │
    │   ├── config/                     ★ QUÂN
    │   │   ├── SecurityConfig.java
    │   │   ├── WebSocketConfig.java
    │   │   ├── CloudinaryConfig.java
    │   │   ├── MailConfig.java
    │   │   └── DataSeeder.java
    │   │
    │   ├── security/                   ★ QUÂN
    │   │   ├── JwtService.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── CustomUserDetailsService.java
    │   │
    │   ├── common/                     ★ QUÂN
    │   │   ├── BaseEntity.java
    │   │   ├── exception/
    │   │   └── util/
    │   │
    │   ├── entity/                     ★ QUÂN chủ trì, cả nhóm review
    │   │   ├── User.java  Role.java  OtpToken.java  Address.java
    │   │   ├── Shop.java  Category.java
    │   │   ├── Product.java  ProductVariant.java  Topping.java
    │   │   ├── Cart.java  CartItem.java
    │   │   ├── Order.java  OrderItem.java  OrderStatus.java
    │   │   ├── Payment.java  PaymentMethod.java
    │   │   ├── Review.java  Wishlist.java  ViewHistory.java
    │   │   ├── Voucher.java  Promotion.java
    │   │   └── Carrier.java  Shipment.java
    │   │
    │   ├── repository/                 (ai sở hữu entity thì viết repository)
    │   │
    │   └── module/
    │       ├── auth/                   ★ QUÂN  đăng ký, OTP, login, quên MK
    │       ├── admin/                  ★ QUÂN  user, danh mục, chiết khấu, carrier
    │       ├── manager/                ★ QUÂN  quản trị theo phạm vi shop
    │       ├── search/                 ★ QUÂN  tìm kiếm & lọc dùng chung
    │       ├── media/                  ★ QUÂN  CloudinaryService
    │       │
    │       ├── catalog/                ◆ HẢI   trang chủ, danh mục, chi tiết SP
    │       ├── cart/                   ◆ HẢI   giỏ hàng lưu DB
    │       ├── checkout/               ◆ HẢI   đặt hàng, áp voucher
    │       ├── account/                ◆ HẢI   profile, địa chỉ, wishlist, đã xem
    │       ├── orderhistory/           ◆ HẢI   lịch sử đơn theo trạng thái
    │       ├── review/                 ◆ HẢI   đánh giá + bình luận ảnh/video
    │       │
    │       ├── vendor/                 ● THIỆN đăng ký shop, SP, đơn, KM, doanh thu
    │       ├── shipper/                ● THIỆN đơn được giao, thống kê
    │       ├── payment/                ● THIỆN Strategy: COD / VNPAY / MOMO
    │       └── realtime/               ● THIỆN WebSocket đẩy trạng thái đơn
    │
    └── resources/
        ├── application.properties           ⚠ dùng chung
        ├── application-local.properties     (mỗi người tự tạo, KHÔNG commit)
        ├── templates/
        │   ├── layout/base.html             ★ QUÂN  ⚠ dùng chung
        │   ├── fragments/                   ★ QUÂN  navbar, footer, pagination
        │   ├── auth/                        ★ QUÂN
        │   ├── admin/                       ★ QUÂN
        │   ├── manager/                     ★ QUÂN
        │   ├── home/  product/              ◆ HẢI
        │   ├── cart/  checkout/             ◆ HẢI
        │   ├── account/  review/            ◆ HẢI
        │   ├── vendor/                      ● THIỆN
        │   └── shipper/                     ● THIỆN
        └── static/
            ├── css/  js/  img/              (đặt tên file theo module: vendor.js, cart.js…)
            └── template-bootstrap/          1 template Bootstrap dùng chung cả nhóm
```

Mỗi module con theo cùng một khuôn, ví dụ `module/cart/`:

```
module/cart/
├── CartController.java        @Controller  trả Thymeleaf
├── CartApiController.java     @RestController  (nếu cần AJAX)
├── CartService.java
└── dto/  CartItemDto.java, AddToCartRequest.java
```

---

## 5. Sơ đồ phụ thuộc giữa 3 khối

```
                   ┌─────────────────────────────────────────┐
                   │   NỀN TẢNG CHUNG  —  QUÂN               │
                   │  entity · repository · SecurityConfig   │
                   │  auth+OTP · search · media · layout     │
                   └───────┬─────────────────────┬───────────┘
                           │  cả nhóm phụ thuộc  │
         ┌─────────────────▼──────┐     ┌────────▼──────────────────┐
         │  GUEST + USER — HẢI    │     │  VENDOR+SHIPPER — THIỆN   │
         │  catalog · cart        │     │  vendor · shipper         │
         │  checkout · account    │◄────┤  payment (Strategy)       │
         │  orderhistory · review │     │  realtime (WebSocket)     │
         └────────────┬───────────┘     └────────┬──────────────────┘
                      │   Order / OrderItem      │
                      └──────────┬───────────────┘
                   ┌─────────────▼──────────────┐
                   │  QUẢN TRỊ — QUÂN           │
                   │  admin · manager           │
                   └────────────────────────────┘
```

Ba **điểm giao** cần thống nhất interface sớm (viết interface trước, implement sau, để không ai phải chờ):

1. `CloudinaryService.upload(MultipartFile) → String url` — Quân viết, Hải & Thiện gọi.
2. `PaymentStrategy.createPayment(Order) → PaymentResult` — Thiện viết, Hải gọi ở checkout.
3. `OrderStatusPublisher.publish(Order)` — Thiện viết, màn lịch sử đơn của Hải nghe.

---

## 6. Quy trình Git

**Nhánh:**

```
main                 ← chỉ merge từ develop, luôn chạy được, dùng để demo/bảo vệ
develop              ← nhánh tích hợp, tất cả PR merge vào đây
feat/quan/auth-otp   ← nhánh làm việc: feat/<tên>/<module>
feat/hai/cart
feat/thien/vendor-product
```

**Vòng làm việc hằng ngày:**

```bash
git checkout develop
git pull origin develop
git checkout -b feat/quan/auth-otp
# ... code ...
git add .
git commit -m "feat(auth): gui OTP qua email khi dang ky"
git push -u origin feat/quan/auth-otp
# mở Pull Request vào develop, nhờ 1 người còn lại review
```

**Luật nhóm (quan trọng, giữ được là không conflict):**

1. **Không sửa file trong thư mục của người khác.** Cần đổi → nhắn vào nhóm chat, để chủ sở hữu sửa.
2. **File dùng chung** — `pom.xml`, `application.properties`, `layout/base.html`, `fragments/`, `SecurityConfig.java`, `entity/` — ai sửa phải báo nhóm trước, commit riêng một commit nhỏ, merge ngay trong ngày.
3. `main` không ai push trực tiếp. Mọi thứ đi qua PR vào `develop`.
4. Mỗi sáng `git pull origin develop` về nhánh mình để conflict nhỏ chứ đừng dồn.
5. **Không commit** mật khẩu DB, JWT secret, key Cloudinary, key VNPAY/MOMO. Dùng `application-local.properties` (đã bỏ vào `.gitignore`) hoặc biến môi trường.
6. Commit message: `feat|fix|docs|refactor(<module>): <mô tả ngắn>`.
7. Cuối mỗi tuần merge `develop` → `main` và kiểm tra app chạy được.

**Thêm vào `.gitignore`:**

```
application-local.properties
/uploads/
*.log
```

---

## 7. Mốc theo tuần

| Tuần | Quân | Hải | Thiện | Mốc chung |
|---|---|---|---|---|
| 1 | ERD + entity + enum 5 vai + CSDL + seed + layout Bootstrap | Trang chủ Guest (>10 SP sắp xếp giảm dần) + trang danh mục | Đăng ký shop + quản lý thông tin shop | App chạy, trang chủ ra sản phẩm từ DB |
| 2 | Đăng ký OTP email, login, logout, quên MK, BCrypt, phân quyền 5 vai | Chi tiết SP + profile + địa chỉ nhận hàng | CRUD sản phẩm của shop + upload Cloudinary | Đăng nhập đúng vai, vào đúng khu vực |
| 3 | Tìm kiếm & lọc dùng chung + CRUD danh mục | **Giỏ hàng DB + checkout COD** | Quản lý đơn của shop theo 7 trạng thái | Đặt được 1 đơn hoàn chỉnh, giá đúng |
| 4 | Dashboard + quản lý user + chiết khấu + carrier | Lịch sử đơn theo trạng thái + voucher | VNPAY/MOMO sandbox + WebSocket đẩy trạng thái | Đổi trạng thái đơn → khách thấy ngay |
| 5 | Khuyến mãi sàn + hoàn thiện Manager | Đánh giá + bình luận (≥50 ký tự, ảnh/video) + wishlist + đã xem | Doanh thu shop + module Shipper | Đủ chức năng đề tài, UI responsive |
| 6 | Báo cáo ERD + bảo mật + Admin/Manager | Báo cáo Guest/User + slide | Báo cáo Vendor/Shipper + pattern + dữ liệu demo | Test end-to-end, tập bảo vệ 1 lần |

---

## 8. Việc tùy chọn (làm nếu còn thời gian)

- Chat real-time giữa User và Vendor qua WebSocket.
- Gợi ý sản phẩm theo lịch sử xem.
- Xuất báo cáo doanh thu ra Excel/PDF.
- Đăng nhập Google OAuth2.
- Thông báo đẩy khi đơn đổi trạng thái.

---

## 9. Checklist đối chiếu đề tài

Trước khi nộp, soát lại từng dòng — đây đúng là các gạch đầu dòng trong đề tài:

- [ ] 1 template Bootstrap dùng chung, giao diện đẹp, responsive
- [ ] Tìm kiếm và lọc sản phẩm
- [ ] Đăng ký tài khoản **có OTP qua Email** để kích hoạt
- [ ] Đăng nhập / đăng xuất
- [ ] Quên mật khẩu **có OTP qua Email**
- [ ] Mật khẩu mã hóa (BCrypt) + dùng Spring Security
- [ ] Guest: trang chủ **>10 sản phẩm** của các shop, **sắp xếp giảm dần**
- [ ] User: 4 block **20 sản phẩm** (mới / bán chạy / đánh giá / yêu thích) **có phân trang hoặc lazy loading**
- [ ] User: profile + **quản lý nhiều địa chỉ nhận hàng**
- [ ] User: **giỏ hàng lưu trên database**
- [ ] User: thanh toán COD + VNPAY hoặc MOMO
- [ ] User: lịch sử mua hàng **theo 6 trạng thái**
- [ ] User: thích sản phẩm / sản phẩm đã xem
- [ ] User: đánh giá sản phẩm **đã mua**
- [ ] User: bình luận **≥50 ký tự** + **ảnh/video**
- [ ] User: chọn mã giảm giá
- [ ] Vendor: đăng ký shop, quản lý trang chủ shop
- [ ] Vendor: quản lý sản phẩm của mình
- [ ] Vendor: quản lý đơn hàng **theo 7 trạng thái**
- [ ] Vendor: tạo chương trình khuyến mãi
- [ ] Vendor: quản lý doanh thu của shop
- [ ] Manager: user, sản phẩm từng shop, danh mục, chiết khấu, khuyến mãi, nhà vận chuyển
- [ ] Admin: như Manager nhưng phạm vi toàn sàn
- [ ] Shipper: đơn được phân công giao + thống kê
- [ ] JWT + WebSocket + Cloudinary đều có dùng thật
- [ ] Báo cáo riêng của từng thành viên theo vai được phân công
