# Hướng dẫn chụp hình cho Chương 5 — Giao diện

Dành cho phần của Quân (xác thực, khu Admin, khu Manager), 17 hình.
Làm theo đúng thứ tự dưới đây thì chụp được hết trong **một lần chạy app**, không phải
khởi động lại giữa chừng.

---

## Bước 0. Chuẩn bị

### 0.1. Bật SQL Server

Mở **SQL Server Configuration Manager** → `SQL Server Services` → `SQL Server (SQLEXPRESS)`
phải đang **Running**. Chưa chạy thì chuột phải → `Start`.

### 0.2. Chạy app trong Spring Tools

1. Mở Spring Tools, ở **Package Explorer** chuột phải vào project `WEB_PROJECT_TEAM17_11`
2. `Run As` → **Spring Boot App**
3. Chờ tới khi cửa sổ **Console** hiện dòng cuối:

```
Started UtetraApplication in ... seconds
```

Nếu Console không hiện, mở bằng `Window` → `Show View` → `Console`.

> **Giữ cửa sổ Console mở suốt buổi chụp.** Hình 5.4 chụp chính cửa sổ này.

Lỗi hay gặp:

| Console báo | Nguyên nhân | Cách sửa |
|---|---|---|
| `Login failed for user` | Sai mật khẩu SQL Server | Sửa `src/main/resources/application-local.properties` |
| `Port 8080 was already in use` | Còn một lần chạy cũ | Bấm nút vuông đỏ **Terminate** trên Console rồi chạy lại |
| Chữ tiếng Việt thành dấu hỏi | Cột đang là `varchar` | Chạy `docs/sql/tao-lai-database.sql` rồi chạy lại app |

### 0.3. Chuẩn bị trình duyệt

Dùng **Chrome**, mở **cửa sổ ẩn danh** (Ctrl + Shift + N) để không dính thanh dấu trang và
tiện đăng nhập nhiều tài khoản.

Đặt đúng chiều rộng bằng công cụ của Chrome thay vì kéo tay:

1. Nhấn **F12** → bấm biểu tượng điện thoại/máy tính bảng ở góc trái thanh công cụ
2. Ô kích thước chọn **Responsive**
3. Gõ **1280 × 800** cho ảnh máy tính, **375 × 812** cho ảnh điện thoại
4. Zoom để **100%**

Chụp xong nhớ nhấn F12 lần nữa để tắt, kẻo panel lập trình lọt vào ảnh.

### 0.4. Công cụ chụp

Nhấn **Win + Shift + S** → chọn vùng → ảnh vào clipboard → dán thẳng vào Word bằng Ctrl + V.
Cắt sát vùng nội dung, **không để lọt thanh tác vụ Windows và thanh địa chỉ trình duyệt**
(trừ khi muốn cho thấy đường dẫn, lúc đó để lại thanh địa chỉ cũng được nhưng phải nhất
quán cả chương).

---

## Bước 1. Luồng đăng ký — Hình 5.1 đến 5.4

Mở http://localhost:8080/register

| Hình | Chụp gì | Cỡ |
|---|---|---|
| **5.1** | Trang đăng ký, biểu mẫu còn trống | 1280 |
| **5.2** | Cùng trang đó | 375 |

Mẹo cho Hình 5.1: trước khi chụp hãy **cố tình bấm Đăng ký khi còn thiếu trường**, để biểu
mẫu hiện lỗi đỏ dưới từng ô. Ảnh có thông báo lỗi nói được nhiều hơn ảnh biểu mẫu trống, và
khớp với câu "hiện lại kèm thông báo ngay dưới ô sai" trong bài.

Sau đó điền thật và gửi:

- Tên đăng nhập: `quanshop`
- Họ tên: `Nguyen Minh Quan`
- Email: `quanshop@utetra.vn`
- Mật khẩu: `123456`

| Hình | Chụp gì | Cỡ |
|---|---|---|
| **5.3** | Trang nhập mã OTP, thấy rõ nút **Gửi lại mã** | 1280 |
| **5.4** | Cửa sổ **Console của Spring Tools**, khối `MA OTP` | — |

Khối trong Console trông như sau, chụp trọn khối:

```
================== OTP (CHE DO THU NGHIEM) ==================
Email nhan : quanshop@utetra.vn
MA OTP     : 965431
=============================================================
```

> Mã in ra Console vì `spring.mail.username` đang để trống — đúng chế độ thử nghiệm mô tả ở
> mục 2.2.1. Không cần cấu hình Gmail.

**Nhập mã đó vào trang nhập mã** để kích hoạt tài khoản `quanshop`. Tài khoản này sẽ dùng ở
Bước 3, đừng bỏ qua.

---

## Bước 2. Luồng đăng nhập và quên mật khẩu — Hình 5.5 đến 5.7

| Hình | Trang | Cỡ |
|---|---|---|
| **5.5** | http://localhost:8080/login | 1280 |
| **5.6** | Cùng trang đó | 375 |
| **5.7** | Ghép hai ảnh `/forgot-password` và `/reset-password` cạnh nhau | 1280 |

Cách làm Hình 5.7: chụp riêng hai trang, dán cả hai vào Word, bôi đen cả hai rồi
`Format` → `Group`, hoặc đơn giản là đặt hai ảnh cạnh nhau trong một bảng 1 hàng 2 cột rồi
ẩn đường kẻ bảng. Để vào `/reset-password` thì nhập email `quanshop@utetra.vn` ở trang quên
mật khẩu trước, mã lại in ra Console.

---

## Bước 3. Tạo một hồ sơ cửa hàng đang chờ duyệt

**Bắt buộc làm bước này**, vì dữ liệu mẫu chỉ có hai cửa hàng đã `ACTIVE`, không có hồ sơ nào
ở trạng thái `PENDING` để chụp Hình 5.9 và 5.10.

1. Đăng nhập bằng `quanshop` / `123456`
2. Vào http://localhost:8080/shop/register
3. Điền tên cửa hàng, ví dụ `UTETra Thu Duc`, địa chỉ và số điện thoại tuỳ ý
4. **Không chọn ảnh logo** — Cloudinary chưa cấu hình trên máy này nên chọn ảnh sẽ lỗi
5. Gửi hồ sơ, rồi **đăng xuất**

Giờ hệ thống có một cửa hàng ở trạng thái `PENDING`.

---

## Bước 4. Khu quản trị — Hình 5.8 đến 5.14

Đăng nhập `admin` / `123456`. Đăng nhập xong hệ thống tự đưa về `/admin`.

| Hình | Trang | Chụp gì | Cỡ |
|---|---|---|---|
| **5.8** | `/admin` | Bảng điều khiển, thấy ô số cửa hàng đang chờ duyệt bằng 1 | 1280 |
| **5.9** | `/admin/shops` | Lọc trạng thái **PENDING**, thấy `UTETra Thu Duc` và các nút kiểm duyệt | 1280 |
| **5.10** | `/admin/shops` | Phóng to đúng hàng của hồ sơ chờ duyệt, thấy ô nhập lý do cạnh nút Từ chối | — |
| **5.11** | `/admin/users` | Gõ `van` vào ô tìm kiếm cho ra vài kết quả, thấy nút Khoá và ô đổi vai | 1280 |
| **5.12** | `/admin/categories` | Cây danh mục, thấy cột đường dẫn không dấu đã sinh | 1280 |
| **5.13** | `/admin/promotions` | Tạo một chương trình trước khi chụp, để có dòng **Đang chạy** màu xanh | 1280 |
| **5.14** | `/admin/shops` | Cùng trang Hình 5.9 | 375 |

Chi tiết vài hình:

**Hình 5.10** — ô nhập lý do nằm **ngay trong hàng của bảng**, không phải hộp thoại bật lên.
Cắt sát lấy một hàng gồm: tên cửa hàng, nút Duyệt, ô nhập lý do, nút Từ chối. Nếu muốn ảnh
nói rõ hơn thì gõ sẵn một lý do vào ô, ví dụ `Thieu giay phep kinh doanh`, rồi mới chụp.

**Hình 5.13** — trước khi chụp hãy tạo một chương trình: tên `Giam 10% toan san`, kiểu
`Phần trăm`, mức `10`, bắt đầu để **hôm qua**, kết thúc để **tuần sau**. Để thời gian như vậy
thì trạng thái mới hiện **Đang chạy**; nếu đặt bắt đầu ở tương lai nó sẽ hiện *Ngoài thời gian*
và ảnh kém thuyết phục.

**Hình 5.14** — ở 375px, cuộn ngang bảng một chút rồi mới chụp, để ảnh cho thấy bảng
**cuộn được** chứ không bị tràn. Đây là bằng chứng cho tiêu chí responsive.

> Sau khi chụp xong Hình 5.9 và 5.10, **bấm Duyệt** cho `UTETra Thu Duc` để thấy chủ cửa hàng
> được nâng vai lên VENDOR. Ảnh này không nằm trong danh sách 17 hình nhưng rất đáng chụp
> thêm cho mục 3.1.2.

---

## Bước 5. Khu Manager — Hình 5.15 đến 5.17

Dữ liệu mẫu chỉ có **một** tài khoản Manager, và cả hai cửa hàng đều do người đó quản lý.
Muốn chụp được Hình 5.17 phải tạo Manager thứ hai. Làm hoàn toàn bằng giao diện, vẫn đang
đăng nhập `admin`:

1. Vào `/admin/users`, tìm tài khoản `khach`, đổi vai thành **MANAGER**
2. Vào `/admin/shops`, ở hàng `UTETra Vo Van Ngan` chọn người quản lý là **khach**
3. Đăng xuất

Giờ `manager` phụ trách `UTETra Le Van Viet`, còn `khach` phụ trách `UTETra Vo Van Ngan`.

Đăng nhập `manager` / `123456`, hệ thống tự đưa về `/manager`.

| Hình | Trang | Chụp gì | Cỡ |
|---|---|---|---|
| **5.15** | `/manager` | Bảng điều khiển, số cửa hàng bằng **1** chứ không phải 2 | 1280 |
| **5.16** | `/manager/products` | Danh sách sản phẩm, thấy nút **Ẩn** ở cột thao tác | 1280 |
| **5.17** | `/manager/shops` | Danh sách chỉ có `UTETra Le Van Viet` | 1280 |

Cho Hình 5.17, chụp thêm một ảnh nữa: đăng xuất, đăng nhập `khach` / `123456`, vào
`/manager/shops`, chụp danh sách chỉ có `UTETra Vo Van Ngan`. Ghép hai ảnh cạnh nhau.

---

## Ba chỗ cần sửa trong file Word

Khi dán hình, sửa luôn mấy chú thích sau cho khớp với cái thật trên màn hình:

| Hình | Chú thích đang ghi | Sửa thành |
|---|---|---|
| **5.10** | Hộp thoại nhập lý do khi từ chối hồ sơ | Ô nhập lý do khi từ chối hồ sơ |
| **5.17** | Manager bị từ chối khi chạm vào cửa hàng ngoài phạm vi | Hai Manager chỉ thấy cửa hàng trong phạm vi của mình |

Lý do sửa Hình 5.17: việc chặn vượt quyền xảy ra khi gửi lệnh `POST`, không gõ được bằng
thanh địa chỉ trình duyệt nên không có màn hình nào để chụp. Bằng chứng cho việc chặn đã nằm
ở **Bảng 4.3** và ở các test tự động rồi. Nếu vẫn muốn một hình về việc chặn, cách tốt hơn là
chụp cửa sổ **JUnit** trong Spring Tools khi chạy `ManagerScopeServiceTest` xanh hết, rồi đưa
vào mục 6.1 thay vì Chương 5.

---

## Bảng kiểm trước khi nộp

- [ ] Đủ 17 hình, không còn dòng xám `[ Chèn hình tại đây … ]` nào
- [ ] Ảnh máy tính đều chụp ở 1280px, ảnh điện thoại đều ở 375px
- [ ] Không ảnh nào dính thanh tác vụ Windows hay panel F12
- [ ] Chữ trong ảnh đọc được khi in ra giấy — in thử một trang để kiểm
- [ ] Chú thích đặt **dưới** hình, in nghiêng, căn giữa, đánh số liên tục 5.1 → 5.17
- [ ] Tên tài khoản trong ảnh là tài khoản mẫu, không phải email hay mật khẩu thật
