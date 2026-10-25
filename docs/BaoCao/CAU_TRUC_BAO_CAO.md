# Cấu trúc báo cáo — Nhóm 17, đề tài 11 (UTETra)

Tài liệu này chốt **cách viết thống nhất** cho bộ báo cáo nộp thầy, gồm:

- **Phần A** — khung chung cho 3 file báo cáo cá nhân (mỗi người một file)
- **Phần B** — sườn bài cho file báo cáo chung của cả nhóm
- **Phần C** — quy ước trình bày áp dụng cho cả 4 file
- **Phần D** — bảng phân công hình ảnh và việc còn lại

Mục đích: ba người viết ra ba file **giống nhau về cấu trúc**, chỉ khác nội dung vai trò. Thầy chấm sẽ thấy ngay đây là một nhóm làm việc có quy củ.

---

# Phần A — Khung chung cho 3 báo cáo cá nhân

**Tên file:** `BaoCao_<Ten>_<Vai>.docx`

| Người | Tên file | Vai trò phụ trách |
|---|---|---|
| Quân | `BaoCao_Quan_Admin_Manager.docx` | Nền tảng chung + Admin + Manager |
| Hải | `BaoCao_Hai_Guest_User.docx` | Guest + User |
| Thiện | `BaoCao_Thien_Vendor_Shipper.docx` | Vendor + Shipper |

**Độ dài:** 15–25 trang. Nội dung đặc, không độn.

## Mục lục chuẩn (cả ba người dùng y hệt)

### 1. Giới thiệu

1.1. Đề tài và bối cảnh — 1 đoạn, nêu UTETra là sàn nhiều shop trà sữa
1.2. Vai trò em phụ trách và lý do phân công như vậy
1.3. Công nghệ sử dụng trong phần của em

### 2. Phân tích chức năng của vai trò

Liệt kê **từng chức năng** trong vai mình, mỗi chức năng một bảng ngắn:

| | |
|---|---|
| Tên chức năng | Đăng ký tài khoản có OTP |
| Người dùng | Khách chưa có tài khoản |
| Đầu vào | Tên đăng nhập, họ tên, email, mật khẩu |
| Xử lý | Kiểm tra trùng, mã hoá BCrypt, sinh OTP, gửi email |
| Đầu ra | Tài khoản ở trạng thái chưa kích hoạt + email chứa mã |
| Ràng buộc | Mật khẩu ≥ 6 ký tự, email chưa ai dùng, OTP hết hạn 5 phút |

### 3. Thiết kế và cài đặt

3.1. **Sơ đồ hoạt động** của 2–3 chức năng tiêu biểu (hình)
3.2. **Các lớp đã viết** — bảng liệt kê Controller / Service / Repository / DTO kèm trách nhiệm từng lớp
3.3. **Giải thích source code** — chọn 3–5 đoạn code đáng nói nhất, chụp hình kèm đánh số dòng và diễn giải bên dưới

> Chọn đoạn nào: ưu tiên chỗ có **quyết định thiết kế**, không phải getter/setter.
> Ví dụ: chỗ kiểm tra chủ sở hữu, chỗ tính tiền, chỗ chuyển trạng thái, chỗ sinh và kiểm OTP.

### 4. Bảo mật và kiểm tra dữ liệu

4.1. Phân quyền: đường dẫn nào, vai nào vào được, chặn ở đâu trong `SecurityConfig`
4.2. Kiểm tra dữ liệu đầu vào: bean validation ở DTO + kiểm tra nghiệp vụ ở Service
4.3. **Chặn vượt quyền** — phần này thầy hay hỏi: chứng minh người dùng A không chạm được dữ liệu của B, kèm đoạn code và kết quả thử

### 5. Giao diện

Mỗi màn hình chính: 1 ảnh desktop + 1 ảnh điện thoại, kèm 2–3 dòng mô tả luồng thao tác.

### 6. Kiểm thử

6.1. Danh sách test tự động đã viết cho phần của mình, mỗi test một dòng nói nó bảo vệ điều gì
6.2. Bảng kịch bản thử tay: thao tác → kết quả mong đợi → kết quả thật

### 7. Khó khăn và cách xử lý

Phần ăn điểm khi thầy hỏi. Mỗi người nêu 2–3 vấn đề **thật sự đã gặp**, nói rõ triệu chứng, nguyên nhân, cách sửa.

Gợi ý từ nhật ký làm việc của nhóm:

- **Quân** — SQL Server lưu mất dấu tiếng Việt vì Hibernate tạo cột `varchar`; `LazyInitializationException` ở trang chủ do `open-in-view=false`; trang công khai trả 404 bị đá về login do quên mở `/error`
- **Hải** — dấu nháy kép chưa escape trong chuỗi làm `develop` không biên dịch được; trùng `@GetMapping("/shop/{id}")` với Thiện khiến app không khởi động nổi (Ambiguous mapping); commit xoá nhầm 15 file của Thiện do chép đè thư mục dự án lên thư mục có `.git`
- **Thiện** — thiết kế máy trạng thái cho 7 trạng thái đơn để chặn nhảy cóc; ký và kiểm chữ ký callback VNPAY/MOMO; chặn xoá sản phẩm đã nằm trong đơn cũ

### 8. Kết luận

8.1. Những gì đã hoàn thành
8.2. Hạn chế còn lại
8.3. Hướng phát triển

---

## Nội dung riêng của từng người

### Quân — Nền tảng chung + Admin + Manager

**Chức năng viết trong mục 2:**

| Nhóm | Chức năng |
|---|---|
| Chung | Đăng ký + OTP email, kích hoạt tài khoản, đăng nhập/đăng xuất, quên mật khẩu + đặt lại, mã hoá BCrypt, phân quyền 5 vai, `POST /api/auth/login` trả JWT |
| Chung | Tìm kiếm và lọc sản phẩm dùng chung, upload ảnh/video lên Cloudinary |
| Admin | Duyệt shop, quản lý user, quản lý danh mục, nhà vận chuyển, khuyến mãi sàn, chiết khấu app |
| Manager | Duyệt shop và quản lý sản phẩm trong phạm vi được phân công |

**Mục 3.1 nên vẽ:** luồng đăng ký + OTP; luồng duyệt shop (PENDING → ACTIVE kèm nâng vai USER → VENDOR)
**Mục 3.3 nên chọn:** `OtpService.consume`, `ShopModerationService.approve`, `requireShopInScope`, `ProductSpecifications`
**Thêm riêng:** phần thiết kế CSDL + sơ đồ ERD (27 entity) — việc này thuộc Quân

### Hải — Guest + User

**Chức năng viết trong mục 2:**

| Nhóm | Chức năng |
|---|---|
| Guest | Trang chủ hiển thị >10 sản phẩm của các shop, sắp xếp giảm dần |
| User | Trang chủ 4 block 20 sản phẩm (mới / bán chạy / đánh giá / yêu thích) có lazy loading |
| User | Trang danh mục có lọc, trang chi tiết sản phẩm (size, topping, đường, đá) |
| User | Hồ sơ cá nhân, đổi mật khẩu, sổ địa chỉ nhận hàng |
| User | Giỏ hàng lưu CSDL nhóm theo shop, thanh toán, áp mã giảm giá |
| User | Lịch sử đơn theo trạng thái, huỷ đơn, trả hàng |
| User | Thích sản phẩm, sản phẩm đã xem, đánh giá 1–5 sao, bình luận ≥50 ký tự kèm ảnh/video |

**Mục 3.1 nên vẽ:** luồng thêm vào giỏ → đặt hàng; luồng đánh giá sản phẩm đã mua
**Mục 3.3 nên chọn:** `CartService` nhóm theo shop, `CheckoutService` tính tiền và áp voucher, chỗ kiểm tra đã mua mới cho đánh giá, chỗ ép bình luận ≥50 ký tự

### Thiện — Vendor + Shipper

**Chức năng viết trong mục 2:**

| Nhóm | Chức năng |
|---|---|
| Vendor | Đăng ký shop, quản lý trang chủ shop |
| Vendor | CRUD sản phẩm, size, topping, upload ảnh Cloudinary |
| Vendor | Quản lý đơn hàng theo 7 trạng thái |
| Vendor | Tạo chương trình khuyến mãi và mã giảm giá của shop |
| Vendor | Báo cáo doanh thu theo ngày/tháng, xuất CSV |
| Shipper | Nhận đơn, cập nhật trạng thái giao, thống kê đơn và tiền COD |
| Chung | Thanh toán VNPAY/MOMO sandbox + callback IPN, WebSocket đẩy trạng thái đơn |

**Mục 3.1 nên vẽ:** máy trạng thái 7 trạng thái đơn; luồng thanh toán VNPAY từ lúc bấm đến lúc nhận IPN
**Mục 3.3 nên chọn:** `OrderStatusTransitions`, chỗ ký và kiểm chữ ký callback, `StompOrderStatusPublisher`

---

# Phần B — Sườn bài báo cáo chung của cả nhóm

**Tên file:** `BaoCao_Nhom17_HuongDanXayDung.docx`
**Độ dài:** 40–60 trang (nhiều hình)

Thầy yêu cầu 6 thứ, sườn dưới đây phủ đủ cả 6:

> hình kiến trúc ứng dụng · sơ đồ hoạt động của chức năng · hình diễn giải source code · hình diễn giải giao diện · hướng dẫn setup · danh sách prompt AI

## Chương 0 — Phần đầu

- Trang bìa: tên trường, môn, đề tài 11, nhóm 17, 3 thành viên kèm MSSV, giảng viên, ngày nộp
- Mục lục tự động
- Danh mục hình, danh mục bảng
- Bảng phân công ghi rõ ai làm gì, kèm **link GitHub**

## Chương 1 — Giới thiệu đề tài

1.1. Bài toán và đối tượng sử dụng
1.2. Phạm vi: mô hình sàn nhiều shop, 5 vai + Guest
1.3. Công nghệ và lý do chọn (bảng)
1.4. Kết quả đạt được — số liệu: 191 file Java, 52 giao diện, 27 entity, 257 test

## Chương 2 — Kiến trúc ứng dụng (hình bắt buộc)

2.1. **Hình 2.1 — Kiến trúc tổng thể**: trình duyệt → Spring MVC → Service → JPA → SQL Server, nhánh phụ sang Cloudinary, VNPAY/MOMO, SMTP, WebSocket
2.2. **Hình 2.2 — Kiến trúc 3 tầng**: Controller → Service → Repository → Entity, kèm DTO
2.3. **Hình 2.3 — Sơ đồ module**: 17 module và ai phụ trách
2.4. **Hình 2.4 — Luồng một request** đi qua các lớp, có chèn Security Filter và JWT filter
2.5. Giải thích vì sao chia theo module nghiệp vụ thay vì chỉ chia theo tầng

## Chương 3 — Thiết kế cơ sở dữ liệu (hình bắt buộc)

3.1. **Hình 3.1 — Sơ đồ ERD** đầy đủ 27 entity
3.2. Mô tả từng bảng: tên, ý nghĩa, khoá chính, khoá ngoại, ràng buộc
3.3. Các enum: `Role`, `OrderStatus` (7 trạng thái), `ShopStatus`, `PaymentMethod`, `DiscountType`
3.4. Quyết định thiết kế đáng nói:
- Vì sao `Shop` là thực thể trung tâm thay vì `Branch`
- Vì sao `Order` chép lại địa chỉ và tên sản phẩm thay vì chỉ giữ khoá ngoại
- Vì sao phải dùng `nvarchar` thay vì `varchar`

## Chương 4 — Bảo mật và phân quyền

4.1. Spring Security: chuỗi filter, form login, `LoginSuccessHandler`
4.2. **Bảng phân quyền**: đường dẫn × vai, ô nào 200 / 302 / 403
4.3. Mã hoá mật khẩu BCrypt
4.4. JWT cho API và WebSocket
4.5. Chống vượt quyền ở tầng nghiệp vụ, không chỉ dựa vào URL
4.6. CSRF, chống lộ email khi quên mật khẩu

## Chương 5 — Hướng dẫn xây dựng từng chức năng (chương dài nhất)

**Khuôn chung cho mỗi chức năng, lặp lại y hệt:**

```
5.x.1  Mô tả chức năng và yêu cầu từ đề tài
5.x.2  Sơ đồ hoạt động                       (HÌNH)
5.x.3  Các lớp tham gia và trách nhiệm        (BẢNG)
5.x.4  Source code chính kèm diễn giải        (HÌNH + giải thích)
5.x.5  Giao diện: desktop + điện thoại        (HÌNH)
5.x.6  Kiểm thử: test tự động + thử tay
```

**Thứ tự các chức năng** — đi theo hành trình người dùng, không theo người làm:

| Mục | Chức năng | Người viết |
|---|---|---|
| 5.1 | Đăng ký + OTP email, kích hoạt | Quân |
| 5.2 | Đăng nhập, đăng xuất, quên mật khẩu | Quân |
| 5.3 | Trang chủ Guest và User, lazy loading | Hải |
| 5.4 | Tìm kiếm và lọc sản phẩm | Quân |
| 5.5 | Trang danh mục, chi tiết sản phẩm | Hải |
| 5.6 | Giỏ hàng lưu CSDL | Hải |
| 5.7 | Thanh toán COD và áp mã giảm giá | Hải |
| 5.8 | Thanh toán VNPAY / MOMO và callback IPN | Thiện |
| 5.9 | Lịch sử đơn, huỷ đơn, trả hàng | Hải |
| 5.10 | Đánh giá và bình luận kèm ảnh/video | Hải |
| 5.11 | Đăng ký shop và duyệt shop | Thiện + Quân |
| 5.12 | Quản lý sản phẩm, size, topping của shop | Thiện |
| 5.13 | Quản lý đơn hàng theo 7 trạng thái | Thiện |
| 5.14 | Khuyến mãi và mã giảm giá của shop | Thiện |
| 5.15 | Báo cáo doanh thu shop | Thiện |
| 5.16 | Shipper nhận đơn, giao hàng, thống kê | Thiện |
| 5.17 | WebSocket đẩy trạng thái đơn real-time | Thiện |
| 5.18 | Khu quản trị: user, danh mục, nhà vận chuyển, khuyến mãi, chiết khấu | Quân |
| 5.19 | Khu Manager theo phạm vi shop | Quân |

## Chương 6 — Hướng dẫn cài đặt và chạy (thầy yêu cầu riêng)

6.1. Yêu cầu môi trường: JDK 17+, SQL Server, không cần cài Maven
6.2. Tạo database, câu lệnh `CREATE DATABASE utetra COLLATE Vietnamese_CI_AS`
6.3. Tạo `application-local.properties`, bảng giải thích từng khoá cấu hình
6.4. Chạy bằng dòng lệnh và chạy bằng Spring Tools (kèm hình)
6.5. Tài khoản mẫu để chấm
6.6. Cấu hình Gmail App Password và Cloudinary; nêu rõ chế độ in OTP ra console khi chưa cấu hình
6.7. Cấu hình VNPAY/MOMO sandbox
6.8. Chạy test: `./mvnw test`
6.9. Xử lý sự cố thường gặp (bảng: triệu chứng → nguyên nhân → cách sửa)

## Chương 7 — Kiểm thử

7.1. Chiến lược: test tự động chạy trên H2, không cần SQL Server
7.2. Bảng 257 test gom theo module, mỗi nhóm nói bảo vệ điều gì
7.3. CI tự chạy test trên mỗi Pull Request (kèm hình dấu tích xanh trên GitHub)
7.4. Bảng kịch bản thử tay cho luồng chính

## Chương 8 — Quy trình làm việc nhóm

8.1. Phân công và lý do chia như vậy
8.2. Quy ước Git: `feat/<tên>/<việc>` → `develop` → `main`
8.3. Luật tránh giẫm chân: mỗi người một thư mục module
8.4. Thống kê: 60 commit, 22 Pull Request, biểu đồ đóng góp (hình chụp Insights)
8.5. Sự cố đã gặp và cách nhóm xử lý

## Chương 9 — Danh sách prompt AI đã dùng (thầy yêu cầu riêng)

Bảng, nhóm theo mục đích:

| STT | Mục đích | Prompt đã dùng | Kết quả AI trả về | Nhóm đã chỉnh gì |
|---|---|---|---|---|
| 1 | Sinh khung entity | ... | ... | ... |

Cuối chương nêu thật: chỗ nào AI làm tốt, chỗ nào AI sai và nhóm phải sửa. Thầy đánh giá cao sự trung thực hơn là tô vẽ.

## Chương 10 — Kết luận

10.1. Đối chiếu với yêu cầu đề tài — bảng 24 yêu cầu, tất cả đạt
10.2. Hạn chế
10.3. Hướng phát triển

## Phụ lục

- A. Bảng toàn bộ đường dẫn và vai được phép truy cập
- B. Cấu trúc thư mục dự án
- C. Link GitHub và hướng dẫn clone

---

# Phần C — Quy ước trình bày (cả 4 file)

| Hạng mục | Quy ước |
|---|---|
| Phông chữ | Times New Roman 13, giãn dòng 1.5 |
| Lề | Trái 3cm, phải 2cm, trên dưới 2cm |
| Tiêu đề | Dùng Heading 1/2/3 của Word để sinh mục lục tự động |
| Đánh số hình | `Hình <chương>.<số>` — ví dụ Hình 5.3 |
| Chú thích hình | Đặt **dưới** hình, căn giữa, in nghiêng |
| Đánh số bảng | `Bảng <chương>.<số>`, chú thích đặt **trên** bảng |
| Source code | Phông Consolas 10, nền xám nhạt, **có đánh số dòng** |
| Ảnh màn hình | Chụp ở 1280px trở lên, cắt gọn, không dính thanh tác vụ |
| Số trang | Góc dưới bên phải, bắt đầu đếm từ Chương 1 |
| Tiếng Việt | Có dấu đầy đủ, viết hoa đúng chuẩn |

**Cách chụp hình source code:** mở file trong Spring Tools, bật số dòng, chọn đúng đoạn cần nói, chụp. Đừng chụp cả file — chỉ 10–25 dòng có ý nghĩa.

**Cách chụp giao diện điện thoại:** mở Chrome, nhấn F12, bấm biểu tượng điện thoại, chọn iPhone SE hoặc đặt 375px, chụp.

---

# Phần D — Phân công hình ảnh và việc còn lại

## Hình cần có trong báo cáo chung

| Hình | Nội dung | Ai làm |
|---|---|---|
| 2.1–2.4 | Kiến trúc tổng thể, 3 tầng, module, luồng request | Quân |
| 3.1 | Sơ đồ ERD 27 entity | Quân |
| 5.x sơ đồ hoạt động | Mỗi chức năng một sơ đồ | Người phụ trách chức năng đó |
| 5.x source code | 3–5 ảnh mỗi chức năng | Người phụ trách |
| 5.x giao diện | Desktop + điện thoại | Người phụ trách |
| 7.3 | Ảnh CI xanh trên GitHub | Quân |
| 8.4 | Ảnh biểu đồ Insights → Contributors | Quân |

**Công cụ vẽ sơ đồ:** draw.io (miễn phí, xuất PNG nền trong). Thống nhất một bộ màu cho cả nhóm để các hình nhìn cùng một kiểu.

## Việc còn lại trước khi nộp

- [ ] Ba người viết xong 3 báo cáo cá nhân theo khung Phần A
- [ ] Gộp nội dung vào báo cáo chung theo sườn Phần B
- [ ] Vẽ các hình trong bảng trên
- [ ] Mỗi người ghi lại prompt AI mình đã dùng cho Chương 9
- [ ] Xuất database ra file `.sql` để nộp kèm
- [ ] Quay video thao tác, có mặt cả 3 người, mỗi người tự giới thiệu và nói phần mình làm
- [ ] Đổi `app.jwt.secret` sang chuỗi mới trước khi nộp source
- [ ] Nộp lên Drive, tạo thư mục tên `17`
- [ ] Nhân bản sheet mẫu, đặt tên `17`, điền đề tài, thành viên và tự chấm điểm
