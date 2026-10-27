# Báo cáo cá nhân

## 1. Giới thiệu

### 1.1. Đề tài và bối cảnh

UTETra là website bán trà sữa cho nhiều shop cùng tham gia. Khách xem sản phẩm, đặt hàng và theo dõi đơn. Chủ shop (Vendor) quản lý shop, sản phẩm, đơn hàng và doanh thu của mình. Quản trị viên (Manager, Admin) giám sát toàn sàn.

Hệ thống có năm vai đã đăng nhập là User, Vendor, Shipper, Manager và Admin. Ngoài ra còn Guest là khách chưa đăng nhập, chỉ xem được trang công khai và không tính vào năm vai trên.

### 1.2. Vai trò em phụ trách và lý do phân công

Em phụ trách bảy nhóm chức năng theo bảng phân công của nhóm.

*Bảng 1.1. Phân công chức năng cho phần em*

| Nhóm | Chức năng trong đề tài |
|---|---|
| Vendor | Đăng ký shop, quản lý trang chủ shop |
| Vendor | CRUD sản phẩm, size, topping, upload ảnh Cloudinary |
| Vendor | Quản lý đơn hàng theo 7 trạng thái |
| Vendor | Tạo chương trình khuyến mãi và mã giảm giá của shop |
| Vendor | Báo cáo doanh thu theo ngày/tháng, xuất CSV |
| Shipper | Nhận đơn, cập nhật trạng thái giao, thống kê đơn và tiền COD |
| Chung | Thanh toán VNPAY/MOMO sandbox, callback IPN, WebSocket đẩy trạng thái đơn |

Ngoài bảy nhóm trên, em viết thêm module dữ liệu demo để sinh đơn hàng mẫu, nhờ vậy biểu đồ doanh thu và các màn hình thống kê có dữ liệu khi trình diễn.

Nhóm giao cả bảy nhóm cho một người vì hai lý do. Thứ nhất, quản lý đơn hàng, doanh thu và thống kê shipper đều đọc chung một bảng đơn nên tách ra sẽ phải sửa qua lại nhiều lần. Thứ hai, thanh toán và WebSocket đều là phần chạy xuyên suốt luồng đặt hàng, giao cho một người phụ trách thì luồng nghiệp vụ liền mạch và ít xung đột khi merge Git.

Riêng phần khuyến mãi và mã giảm giá, mã nguồn do bạn Quân viết. Em giữ mục 2.5 trong báo cáo để mô tả cho đủ bộ chức năng của Vendor, không trình bày phần cài đặt chi tiết.

### 1.3. Công nghệ sử dụng trong phần của em

*Bảng 1.2. Công nghệ sử dụng trong phần của em*

| Công nghệ | Vai trò trong phần của em |
|---|---|
| Spring Boot + Spring MVC | Cấu trúc Controller/Service, REST cho IPN thanh toán |
| Thymeleaf + Bootstrap | Giao diện các trang Vendor, Shipper, Thanh toán |
| Spring Data JPA + SQL Server | Truy vấn đơn hàng, sản phẩm, doanh thu |
| Spring Security | Kế thừa phân quyền 5 vai của nhóm |
| WebSocket + STOMP (SockJS) | Đẩy trạng thái đơn realtime |
| Cloudinary (qua `CloudinaryService`) | Upload logo/banner shop, ảnh sản phẩm |
| VNPAY / MOMO sandbox | Tạo URL thanh toán, xử lý callback/IPN |
| HMAC-SHA512 / HMAC-SHA256 | Ký và kiểm tra chữ ký callback |

## 2. Phân tích chức năng của vai trò

### 2.1. Đăng ký shop

Khách đã đăng nhập vào `/shop/register`, điền tên shop, địa chỉ, số điện thoại, mô tả và tải logo. Shop mới luôn được tạo ở trạng thái PENDING. Tài khoản người đăng ký vẫn giữ vai User cho tới khi Manager hoặc Admin duyệt, lúc đó mới được nâng lên Vendor. Cách làm này tránh trường hợp tự đăng ký xong là có quyền bán hàng ngay.

Logo shop đi qua `ImageUploadValidator` trước khi đưa lên Cloudinary.

### 2.2. Quản lý trang chủ shop

Chủ shop sửa thông tin shop tại `/vendor/shop`: tên, địa chỉ, điện thoại, mô tả, logo và ảnh banner. Form cập nhật dùng `ShopUpdateRequest`, cố tình không có trường `owner`, `status` và `commissionRate`, nên chủ shop không thể tự đổi chủ sở hữu, tự duyệt shop hay tự giảm chiết khấu.

Trang công khai `/shop/{id}` cho phép mọi người xem, kể cả chưa đăng nhập.

### 2.3. Quản lý sản phẩm, size, topping và upload ảnh

Chủ shop thêm, sửa, xoá sản phẩm tại `/vendor/products`. Mỗi sản phẩm có nhiều size, mỗi size có giá riêng, và có thể gắn thêm topping. Topping quản lý riêng ở `/vendor/toppings`.

Sản phẩm không bị xoá cứng nếu đã nằm trong đơn hàng, giỏ hàng, danh sách yêu thích hoặc đánh giá. Khi đó hệ thống yêu cầu dùng chức năng tắt bán để giữ lại lịch sử đơn.

Ảnh sản phẩm và logo đi qua `ImageUploadValidator` rồi mới lên Cloudinary. Phần kiểm tra này trình bày ở mục 4.2.

### 2.4. Quản lý đơn hàng của shop theo 7 trạng thái

Đơn hàng đi qua bảy trạng thái: NEW, CONFIRMED, PICKED_UP, SHIPPING, DELIVERED, CANCELLED và RETURNED_REFUNDED. Luồng chính là NEW, CONFIRMED, PICKED_UP, SHIPPING, DELIVERED.

Ngoài luồng chính còn ba nhánh. Thứ nhất, đơn ở NEW hoặc CONFIRMED có thể huỷ, chuyển sang CANCELLED. Thứ hai, đơn đang SHIPPING giao thất bại thì chuyển sang RETURNED_REFUNDED. Thứ ba, đơn đã DELIVERED vẫn có thể chuyển sang RETURNED_REFUNDED khi khách trả hàng sau khi nhận.

CANCELLED và RETURNED_REFUNDED là trạng thái kết thúc, không chuyển tiếp được nữa.

Mỗi lần đổi trạng thái đều đi qua `OrderStatusService`, nơi kiểm tra vai đang thao tác có được phép chuyển bước đó không. Chủ shop chỉ đổi được đơn của shop mình.

### 2.5. Tạo chương trình khuyến mãi và mã giảm giá của shop

Chủ shop tạo chương trình khuyến mãi và mã giảm giá tại `/vendor/promotions`. Mỗi chương trình có thời gian áp dụng, điều kiện đơn tối thiểu và mức giảm. Chủ shop bật hoặc tắt chương trình, và xoá mềm chương trình chưa bị đơn nào dùng tới.

Phần mã nguồn của chức năng này do bạn Quân viết, gồm `VendorPromotionController` và `VendorPromotionService`. Em đưa vào báo cáo để mô tả đủ bộ chức năng Vendor theo bảng phân công, không đi vào chi tiết cài đặt.

### 2.6. Báo cáo doanh thu shop và xuất CSV

Trang `/vendor/revenue` nhận khoảng ngày và cách gộp (theo ngày hoặc theo tháng), trả về bốn con số: doanh thu các đơn đã giao, hoa hồng sàn, thực nhận và số đơn đã giao trên tổng số đơn. Bên dưới là biểu đồ doanh thu theo thời gian, bảng món bán chạy và bảng chi tiết từng ngày.

Nút tải CSV gọi `/vendor/revenue/export.csv`, xuất đúng khoảng ngày đang xem để chủ shop đối chiếu ngoài Excel.

Cách tính: chỉ cộng các đơn ở trạng thái DELIVERED. Hoa hồng lấy theo `commissionRate` của shop, thực nhận là phần còn lại sau khi trừ hoa hồng.

### 2.7. Shipper: nhận đơn, giao hàng và thống kê COD

Shipper vào `/shipper/available` xem các đơn đã được shop xác nhận và chưa có ai nhận, bấm nhận đơn thì đơn chuyển sang PICKED_UP và gắn với shipper đó. Danh sách đơn đang giao ở `/shipper/orders`, chi tiết ở `/shipper/orders/{id}`.

Khi giao, shipper cập nhật sang SHIPPING rồi DELIVERED. Nếu giao thất bại, shipper chuyển sang RETURNED_REFUNDED và bắt buộc nhập lý do.

Với đơn COD, khi shipper đánh dấu DELIVERED thì hệ thống tự chuyển trạng thái thanh toán sang PAID, vì lúc đó shipper đã thu tiền của khách.

Trang `/shipper/stats` tổng hợp số đơn đã giao, số đơn thất bại và tổng tiền COD đã thu.

### 2.8. Thanh toán VNPAY/MOMO (Strategy + callback IPN)

Ba phương thức COD, VNPAY và MOMO cùng cài đặt interface `PaymentStrategy`. `PaymentStrategyRegistry` chọn đúng strategy theo `PaymentMethod` của đơn, nhờ vậy thêm cổng thanh toán mới chỉ cần viết thêm một lớp.

Chọn VNPAY hoặc MOMO thì hệ thống dựng URL thanh toán có kèm chữ ký: VNPAY dùng HMAC-SHA512, MOMO dùng HMAC-SHA256. Trình duyệt chuyển sang trang sandbox của cổng.

Cổng gọi lại hai đường dẫn. Return URL đưa người dùng về `/payment/result` để xem kết quả. IPN là kênh máy chủ gọi máy chủ, dùng để cập nhật trạng thái thanh toán chính thức. Cả hai đều kiểm tra chữ ký trước khi làm bất cứ việc gì.

Cổng chưa được cấu hình khoá thì hệ thống không dựng URL và chỉ ghi log, ứng dụng vẫn chạy bình thường. Riêng trường hợp này IPN đang trả `RspCode 00` thay vì mã lỗi, khác với nguyên tắc từ chối nêu trên; chi tiết ở mục 7.2 và 8.2.

### 2.9. WebSocket realtime đẩy trạng thái đơn

Mỗi lần trạng thái đơn đổi, `OrderStatusService` gọi `OrderStatusPublisher`. Bản thật là `StompOrderStatusPublisher`, đẩy sự kiện qua ba kênh:

*Bảng 2.1. Kênh WebSocket theo vai*

| Kênh | Người nhận |
|---|---|
| `/user/{username}/queue/orders` | Khách đã đặt đơn đó |
| `/topic/shop/{shopId}/orders` | Mọi người trong shop |
| `/user/{username}/queue/deliveries` | Shipper đang giao đơn |

Hai kênh `/user/...` là kênh riêng của từng tài khoản, Spring tự thêm tiền tố và chỉ người đăng nhập đúng username mới nhận được. Nhờ vậy khách này không thấy đơn của khách khác.

Nhờ WebSocket, khách không cần tải lại trang mà vẫn thấy trạng thái đơn đổi, và shop thấy đơn mới ngay khi khách đặt.

## 3. Thiết kế và cài đặt

### 3.1. Sơ đồ hoạt động của hai chức năng tiêu biểu

**Sơ đồ trạng thái đơn hàng.** Hình 3.1 vẽ lại bảy trạng thái và các đường chuyển hợp lệ. Sơ đồ khớp với mô tả ở mục 2.4, gồm cả nhánh DELIVERED sang RETURNED_REFUNDED.

[!HINH 3.1]

Nhìn sơ đồ có thể thấy hai trạng thái CANCELLED và RETURNED_REFUNDED không có mũi tên đi ra. Đó là chủ ý: đơn đã kết thúc thì không đổi trạng thái được nữa.

**Luồng thanh toán VNPAY.** Hình 3.2 đi từ lúc khách bấm đặt hàng đến lúc hệ thống nhận IPN.

[!HINH 3.2]

Sơ đồ trên mô tả trường hợp cổng đã được cấu hình khoá. Khi cổng chưa cấu hình, IPN hiện trả `RspCode 00` thay vì mã lỗi; chi tiết ở mục 7.2 và 8.2.

Các bước của luồng:

1. Người dùng chọn VNPAY khi đặt hàng.
2. `PaymentService` chọn `VnPayPaymentStrategy`, ký tham số bằng HMAC-SHA512 và tạo URL sandbox.
3. Trình duyệt chuyển hướng sang trang sandbox VNPAY.
4. Sau khi thanh toán, VNPAY gọi URL return để hiển thị kết quả và URL IPN để xử lý chính thức.
5. `PaymentIpnController` kiểm tra chữ ký, tìm đơn, so số tiền rồi cập nhật trạng thái thanh toán.

Điểm cần chú ý là bước 5 kiểm tra cả số tiền, không chỉ chữ ký. Nếu khách sửa số tiền trên URL, chữ ký sẽ không khớp, hoặc nếu khớp mà số tiền khác đơn thì hệ thống vẫn từ chối.

### 3.2. Các lớp đã viết

Các lớp em viết gom theo module trong một bảng. Hai lớp `VendorPromotionController` và `VendorPromotionService` của module vendor do bạn Quân viết, em không tính vào phần của mình nên không liệt kê ở đây.

*Bảng 3.1. Các lớp đã viết, gom theo module*

| Module | Lớp | Loại | Trách nhiệm |
|---|---|---|---|
| vendor | `ShopRegistrationController` | Controller | Form đăng ký shop |
| vendor | `VendorShopController` | Controller | Sửa thông tin shop |
| vendor | `VendorShopService` | Service | Quyền sở hữu (`requireOwnedShop`) |
| vendor | `ShopPublicController` | Controller | Trang công khai `/shop/{id}` |
| vendor | `VendorHomeController` | Controller | Dashboard `/vendor` |
| vendor | `VendorProductController` | Controller | Danh sách, thêm/sửa/xoá sản phẩm |
| vendor | `VendorProductService` | Service | Nghiệp vụ sản phẩm, chặn xoá, IDOR |
| vendor | `VendorToppingController` | Controller | Danh sách và form topping |
| vendor | `VendorToppingService` | Service | Nghiệp vụ topping, chặn xoá |
| vendor | `VendorOrderController` | Controller | Danh sách và chi tiết đơn |
| vendor | `VendorOrderService` | Service | Lọc đơn, gọi `OrderStatusService` |
| vendor | `VendorRevenueController` | Controller | Doanh thu và xuất CSV |
| vendor | `VendorRevenueService` | Service | Doanh thu, chiết khấu, gộp theo ngày |
| vendor | `ImageUploadValidator` | Validator | Kiểm magic bytes ảnh |
| vendor | `VendorPromotionController` | Controller | Trang khuyến mãi, mã giảm giá |
| vendor | `VendorPromotionService` | Service | Nghiệp vụ promotion, voucher |
| shipper | `ShipperHomeController` | Controller | Dashboard `/shipper`, thống kê nhanh |
| shipper | `ShipperOrderController` | Controller | Đơn chờ nhận, đơn của tôi, chi tiết, thống kê |
| shipper | `ShipperOrderService` | Service | Nhận đơn, cập nhật giao hàng, thống kê COD, chặn IDOR |
| orderstatus | `OrderActor` | Enum | Vai đang thực hiện chuyển trạng thái |
| orderstatus | `OrderStatusTransitions` | Bảng luật | Quy tắc chuyển trạng thái hợp lệ theo từng vai |
| orderstatus | `OrderStatusService` | Service | Một nơi duy nhất đổi trạng thái và chạy tác dụng phụ |
| payment | `PaymentStrategy` | Interface | Hợp đồng chung cho mọi phương thức thanh toán |
| payment | `CodPaymentStrategy` | Strategy | Xử lý COD: thu tiền khi giao thành công |
| payment | `VnPayPaymentStrategy` | Strategy | Ký HMAC-SHA512, dựng URL sandbox VNPAY |
| payment | `MomoPaymentStrategy` | Strategy | Ký HMAC-SHA256, dựng request sandbox MOMO |
| payment | `PaymentStrategyRegistry` | Registry | Chọn strategy theo `PaymentMethod` |
| payment | `PaymentGatewayProperties` | Config | Nạp key cấu hình cổng, mặc định rỗng |
| payment | `PaymentService` | Service | Nghiệp vụ thanh toán dùng chung, xử lý callback |
| payment | `PaymentController` | Controller | Endpoint checkout, return |
| payment | `PaymentIpnController` | Controller | Nhận IPN từ VNPAY/MOMO |
| payment | `PaymentSecurityConfig` | Config | Cấu hình bảo mật riêng cho `/payment/**` |
| payment | `PaymentSignatures` | Tiện ích | Ký và kiểm HMAC, encode URL, so sánh constant-time |
| realtime | `OrderStatusPublisher` | Interface | Hợp đồng đẩy sự kiện trạng thái |
| realtime | `StompOrderStatusPublisher` | Publisher | Đẩy sự kiện trạng thái đơn qua STOMP |
| realtime | `OrderStatusEvent` | Model | Dữ liệu sự kiện đẩy đi |
| demo | `DemoOrderSeeder` | Seeder | Sinh đơn hàng demo cho biểu đồ và màn hình |

### 3.3. Giải thích source code

Phần này chọn năm đoạn mã, mỗi đoạn là chỗ em cho rằng quyết định thiết kế quan trọng nhất. Ảnh chụp lấy từ VS Code, có số dòng.

#### 3.3.1. OrderStatusTransitions

[!HINH 3.3]

Đoạn mã khai báo bảng luật dưới dạng hai `Map`. Bảng thứ nhất `ALLOWED` trả lời câu hỏi "trạng thái này đi được sang trạng thái nào", chưa xét ai được làm. Bảng thứ hai `ALLOWED_BY_ACTOR` trả lời tiếp "vai nào được chuyển bước đó".

Tách hai bảng vì có bước nhiều vai cùng làm được: huỷ đơn thì cả khách và chủ shop đều được, còn xác nhận đơn chỉ chủ shop. Gộp một bảng thì phải lặp lại luật ở nhiều chỗ.

Cả hai bảng đều bọc trong `Collections.unmodifiableMap` để không ai sửa được luật lúc chạy.

#### 3.3.2. OrderStatusService

[!HINH 3.4]

Phương thức `change` là cửa duy nhất để đổi trạng thái đơn. Nó chặn bốn trường hợp: thiếu trạng thái mới, chuyển sang chính trạng thái đang có, chuyển bước không hợp lệ, và đổi trạng thái của đơn đã kết thúc.

Sau khi đặt trạng thái mới, hàm gọi `applySideEffects` để chạy việc phụ thuộc trạng thái, ví dụ đơn COD giao xong thì đánh dấu đã thanh toán. Cuối cùng đơn được lưu và đẩy sự kiện qua `orderStatusPublisher`.

Gom về một chỗ tránh được việc mỗi controller tự đổi trạng thái theo cách riêng, và bảo đảm đơn nào đổi cũng có thông báo realtime.

#### 3.3.3. PaymentSignatures

[!HINH 3.5]

Lớp này gom hai việc dễ lệch nhau là ký lúc tạo yêu cầu và kiểm chữ ký lúc nhận callback. Viết riêng hai chỗ thì chỉ cần lệch thứ tự tham số là chữ ký sai.

Ba phương thức `hmacSha512`, `hmacSha256` và `hmac` dùng chung một cách mã hoá và một thứ tự tham số. Hàm `urlEncode` mã hoá theo RFC 3986, đổi dấu cộng thành `%20` vì VNPAY và MOMO đều tính chữ ký trên chuỗi đã mã hoá.

#### 3.3.4. StompOrderStatusPublisher

[!HINH 3.6]

Lớp cài đặt `OrderStatusPublisher` và đẩy sự kiện qua `SimpMessagingTemplate`. Hai hằng số `CUSTOMER_QUEUE` và `SHIPPER_QUEUE` là kênh riêng của từng tài khoản, còn kênh shop là `/topic/shop/{shopId}/orders` để mọi người trong shop cùng nghe.

Kênh `/user/...` do Spring tự thêm tiền tố, nên chỉ người đăng nhập đúng username mới nhận được. Đây là lý do em chọn kênh riêng cho khách thay vì một topic chung.

#### 3.3.5. ImageUploadValidator

[!HINH 3.7]

Lớp kiểm tra file trước khi đưa lên Cloudinary, gồm bốn lớp kiểm tra: kích thước không quá 5MB, Content-Type nằm trong danh sách trắng, đuôi file nằm trong danh sách trắng, và magic bytes của nội dung file phải đúng định dạng ảnh.

Lớp thứ tư quan trọng nhất: kẻ tấn công đổi tên file `.exe` thành `.png` và khai Content-Type `image/png` thì mười hai byte đầu vẫn không khớp định dạng ảnh nào.

Em đặt phần kiểm tra ở tầng nghiệp vụ chứ không sửa `CloudinaryService` vì đó là file dùng chung của nhóm, đồng thời báo được lỗi rõ ràng thay vì trả về 500.

## 4. Bảo mật và kiểm tra dữ liệu

### 4.1. Phân quyền theo đường dẫn

Phần lớn đường dẫn của em đi qua `SecurityConfig` dùng chung của nhóm. Bảng dưới là theo cấu hình hiện tại trong `src/main/java/com/utetra/security/SecurityConfig.java`, cột cuối ghi tên phương thức cấu hình.

*Bảng 4.1. Phân quyền theo đường dẫn*

| Đường dẫn | Vai được phép | Cấu hình |
|---|---|---|
| `/shop/**` | Tất cả, kể cả chưa đăng nhập | `permitAll()` để khách xem trang shop công khai |
| `/vendor/**` | `ADMIN`, `VENDOR` | `hasAnyRole("ADMIN", "VENDOR")` |
| `/shipper/**` | `ADMIN`, `SHIPPER` | `hasAnyRole("ADMIN", "SHIPPER")` |
| `/payment/**` | Xem giải thích phía dưới | `PaymentSecurityConfig` của em, `@Order(1)` |

Riêng `/payment/**` em không sửa `SecurityConfig` dùng chung mà tách một cấu hình riêng `PaymentSecurityConfig` ghép trước chuỗi filter của nhóm:

```java
http
    .securityMatcher("/payment/**")
    .authorizeHttpRequests(auth -> auth
            .requestMatchers("/payment/vnpay/return",
                             "/payment/vnpay/ipn",
                             "/payment/momo/return",
                             "/payment/momo/ipn").permitAll()
            .anyRequest().authenticated())
```

Lý do phải mở `permitAll()` cho bốn đường dẫn callback: máy chủ VNPAY và MOMO gọi thẳng vào đó, không có phiên đăng nhập và không mang theo cookie. Nếu để `authenticated()` thì mọi IPN đều bị chặn và chuyển về trang đăng nhập, đơn không bao giờ được cập nhật thanh toán.

Bù lại, bốn đường dẫn đó tự kiểm tra chữ ký HMAC trước khi làm bất cứ việc gì, nên bên ngoài không có khoá thì không sửa được trạng thái đơn.

Điều đáng nói: `ADMIN` cũng vào được `/vendor/**` và `/shipper/**`. Đây là chủ ý của nhóm để quản trị viên kiểm tra dữ liệu khi vận hành, nhưng nghĩa là phân quyền theo URL không đủ. Vì vậy em vẫn giữ kiểm tra chủ sở hữu ở tầng Service, trình bày ở mục 4.3.

### 4.2. Kiểm tra dữ liệu đầu vào

**DTO không cho phép mass assignment.** `ShopUpdateRequest` không có trường `owner`, `status`, `commissionRate` nên Vendor không thể tự nâng quyền, tự duyệt shop hoặc tự đổi chiết khấu.

**Bean validation.** Các form sản phẩm và topping được validate: bắt buộc, độ dài, giá trị hợp lệ.

**Kiểm tra nghiệp vụ trong Service.** Ảnh upload đi qua `ImageUploadValidator` như trình bày ở mục 3.3.5. Bước chuyển trạng thái đi qua `OrderStatusTransitions`.

### 4.3. Chặn vượt quyền

Điểm quan trọng nhất là chống IDOR: người dùng A không được chạm dữ liệu của B dù có sửa id trên URL.

[!HINH 4.1]

Hai hàm `requireOwnedShop` và `requireAssignedOrder` là cửa chung của mọi thao tác ghi. Hàm tìm bản ghi trước, nếu không có thì trả 404. Nếu có nhưng chủ sở hữu khác người đang đăng nhập thì ném `AccessDeniedException` và trả 403.

Mọi thao tác sửa shop, sản phẩm, đơn của Vendor đều đi qua `requireOwnedShop` trước khi ghi. Phía shipper dùng `requireAssignedOrder` để chặn shipper khác lấy được đơn không phải của mình.

Kết quả thử tay hai trường hợp này ghi ở mục 6.2 dòng 11 và dòng 12. Test tự động tương ứng nằm trong `ShipperOrderServiceTest` và `ShipperOrderWebTest`, chi tiết ở mục 6.1.

## 5. Giao diện

Phần này mô tả các màn hình em làm. Ảnh desktop chụp ở độ phân giải 1440px. Ảnh điện thoại chụp trên máy tính bằng chế độ responsive của Chrome (F12), đặt độ rộng 390px. Mỗi màn chính có một cặp ảnh kèm mô tả luồng thao tác. Tên template Thymeleaf của từng màn trùng với tên file trong thư mục `templates`, ví dụ `/vendor/shop` dùng `vendor/shop-form.html`.

### 5.1. Đăng ký shop

*Đường dẫn:* `/shop/register`. Khách đã đăng nhập điền tên shop, địa chỉ, số điện thoại, mô tả và chọn logo. Sau khi gửi, shop ở trạng thái chờ duyệt. Màn hình có ghi rõ điều kiện này ngay dưới tiêu đề để người dùng biết vì sao chưa vào được khu quản lý.

[!UI_TRANG 5.1]

### 5.2. Trang công khai shop

*Đường dẫn:* `/shop/{id}`. Trang này ai cũng xem được. Đầu trang là tên, địa chỉ và điện thoại của shop, bên dưới là danh sách sản phẩm kèm giá và số lượng đã bán. Mỗi thẻ sản phẩm dẫn sang trang chi tiết để chọn size và topping.

[!UI_TRANG 5.2]

### 5.3. Dashboard Vendor

*Đường dẫn:* `/vendor`. Trang tổng quan gồm các ô số liệu: số sản phẩm đang bán, số topping, số đơn hàng của shop, doanh thu. Phía dưới là các nút dẫn nhanh sang sản phẩm, topping, khuyến mãi và đơn hàng, cùng bảng đơn hàng theo trạng thái.

[!UI_TRANG 5.3]

### 5.4. Sửa thông tin shop

*Đường dẫn:* `/vendor/shop`. Form sửa tên, địa chỉ, điện thoại, mô tả, logo và banner. Form này dùng chung giao diện với trang đăng ký shop nhưng dữ liệu đã được nạp sẵn và không có trường trạng thái hay chiết khấu.

[!UI_TRANG 5.4]

### 5.5. Danh sách sản phẩm

*Đường dẫn:* `/vendor/products`. Bảng liệt kê sản phẩm của shop với ảnh, giá, số size và trạng thái bán. Mỗi dòng có nút sửa, nút bật/tắt bán và nút xoá. Nút xoá sẽ báo lỗi nếu sản phẩm đã nằm trong đơn hàng.

[!UI_TRANG 5.5]

### 5.6. Form thêm sản phẩm

*Đường dẫn:* `/vendor/products/new`. Form gồm thông tin cơ bản, ảnh sản phẩm, danh sách size kèm giá và danh sách topping gắn kèm. Ảnh được kiểm tra định dạng ngay khi tải lên, sai định dạng thì báo lỗi tại chỗ chứ không đợi tới lúc lưu.

[!UI_TRANG 5.6]

### 5.7. Quản lý topping

*Đường dẫn:* `/vendor/toppings`. Bảng topping của shop gồm tên, giá và trạng thái. Topping đang nằm trong giỏ hàng của khách thì không xoá được, hệ thống yêu cầu dùng chức năng tắt bán.

[!UI_TRANG 5.7]

### 5.8. Danh sách đơn hàng

*Đường dẫn:* `/vendor/orders`. Đầu trang có nút lọc theo bảy trạng thái, mỗi nút kèm số đơn. Bảng bên dưới hiện mã đơn, khách nhận, tổng tiền, phương thức thanh toán và trạng thái. Bấm mã đơn để mở trang chi tiết.

[!UI_TRANG 5.8]

### 5.9. Chi tiết đơn hàng phía Vendor

*Đường dẫn:* `/vendor/orders/{id}`. Trang này hiển thị thông tin người nhận, danh sách món kèm topping, tổng tiền và lịch sử trạng thái. Các nút đổi trạng thái chỉ hiện đúng những bước mà đơn được phép chuyển, danh sách lấy từ `OrderStatusTransitions`.

[!UI_DON 5.9]
![Giao diện Chi tiết đơn hàng phía Vendor](/tmp/figs/out/ph_vendor_detail.png)

### 5.10. Khuyến mãi và mã giảm giá

*Đường dẫn:* `/vendor/promotions`. Danh sách chương trình khuyến mãi với thời gian áp dụng, mức giảm và trạng thái bật/tắt. Giao diện do bạn Quân làm, em đưa vào cho đủ bộ chức năng Vendor.

[!UI_TRANG 5.10]

### 5.11. Báo cáo doanh thu

*Đường dẫn:* `/vendor/revenue`. Chọn khoảng ngày và cách gộp rồi bấm xem báo cáo. Bốn ô số liệu ở trên cùng cho biết doanh thu, hoa hồng, thực nhận và số đơn đã giao. Bên dưới là biểu đồ cột theo thời gian, bảng món bán chạy và bảng chi tiết từng ngày.

[!UI_TRANG 5.11]

### 5.12. Dashboard Shipper

*Đường dẫn:* `/shipper`. Các ô số liệu cho shipper biết đang có bao nhiêu đơn chờ nhận, bao nhiêu đơn đang giao và tổng tiền COD đã thu. Phía dưới là các nút dẫn sang đơn chờ nhận, đơn của tôi và thống kê.

[!UI_TRANG 5.12]

### 5.13. Đơn chờ nhận

*Đường dẫn:* `/shipper/available`. Danh sách đơn đã được shop xác nhận và chưa có shipper nhận. Mỗi đơn hiện địa chỉ giao, tiền cần thu và nút nhận đơn. Bấm nhận thì đơn chuyển sang PICKED_UP và biến khỏi danh sách của shipper khác.

[!UI_TRANG 5.13]

### 5.14. Đơn của tôi

*Đường dẫn:* `/shipper/orders`. Danh sách đơn shipper đang phụ trách, lọc theo trạng thái. Mỗi dòng có nút cập nhật trạng thái giao. Khi chuyển sang trả hàng, hệ thống bắt buộc nhập lý do.

[!UI_TRANG 5.14]

### 5.15. Chi tiết đơn hàng phía Shipper

*Đường dẫn:* `/shipper/orders/{id}`. Trang chi tiết một đơn shipper đang giao: danh sách món, số điện thoại và địa chỉ người nhận, tiền thu hộ COD, cùng các nút cập nhật trạng thái. Shipper cập nhật sang SHIPPING khi bắt đầu đi giao, sang DELIVERED khi giao xong, hoặc sang RETURNED_REFUNDED khi giao thất bại và phải nhập lý do.

[!UI_DON 5.15]
![Giao diện Chi tiết đơn hàng phía Shipper](/tmp/figs/out/ph_shipper_detail.png)

### 5.16. Thống kê COD

*Đường dẫn:* `/shipper/stats`. Tổng hợp số đơn đã giao, số đơn thất bại và tổng tiền COD đã thu trong khoảng thời gian chọn. Bảng chi tiết bên dưới cho biết từng đơn đóng góp bao nhiêu vào con số tổng.

[!UI_TRANG 5.16]

### 5.17. Chọn phương thức thanh toán

*Đường dẫn:* `/payment/checkout/{orderId}`. Trang này hiện ba lựa chọn COD, VNPAY và MOMO cho đơn chưa thanh toán. Chọn COD thì đơn giữ nguyên trạng thái chờ thanh toán; chọn VNPAY hoặc MOMO thì hệ thống gọi strategy tương ứng để dựng URL có kèm chữ ký rồi chuyển trình duyệt sang trang sandbox của cổng.

[!UI_DON 5.17]
![Giao diện Chọn phương thức thanh toán](/tmp/figs/out/ph_payment_checkout.png)

### 5.18. Kết quả thanh toán

*Đường dẫn:* `/payment/result`. Trang nhận các tham số cổng trả về qua Return URL, kiểm tra chữ ký rồi hiển thị kết quả thành công hay thất bại. Trạng thái thanh toán của đơn chỉ đổi sau khi IPN gọi tới máy chủ, nên trang này chỉ có nhiệm vụ hiển thị.

[!UI_DON 5.18]
![Giao diện Kết quả thanh toán](/tmp/figs/out/ph_payment_result.png)

## 6. Kiểm thử

### 6.1. Test tự động

Chạy toàn bộ test bằng `./mvnw test`, không cần cài Maven riêng.

Kết quả chạy toàn dự án: 257 test, tất cả đều qua, không có test lỗi hay bị bỏ qua. Phần em viết là 163 test thuộc 22 lớp liệt kê ở bảng 6.1 dưới đây; phần còn lại của nhóm, trong đó có `VendorPromotionServiceTest` của bạn Quân, nằm ngoài phạm vi báo cáo này.

*Bảng 6.1. Các lớp test của phần em*

| Module | Lớp test | Số test |
|---|---|---|
| vendor | `VendorShopServiceTest` | 9 |
| vendor | `VendorSecurityWebTest` | 7 |
| vendor | `VendorProductServiceTest` | 8 |
| vendor | `VendorProductWebTest` | 6 |
| vendor | `VendorToppingServiceTest` | 4 |
| vendor | `VendorOrderServiceTest` | 11 |
| vendor | `VendorOrderWebTest` | 5 |
| vendor | `VendorRevenueServiceTest` | 11 |
| vendor | `VendorRevenueWebTest` | 5 |
| vendor | `ImageUploadValidatorTest` | 7 |
| shipper | `ShipperOrderServiceTest` | 13 |
| shipper | `ShipperOrderWebTest` | 10 |
| orderstatus | `OrderStatusServiceTest` | 8 |
| orderstatus | `OrderStatusTransitionsTest` | 5 |
| payment | `PaymentServiceTest` | 12 |
| payment | `PaymentWebTest` | 10 |
| payment | `PaymentSignaturesTest` | 6 |
| payment | `VnPayPaymentStrategyTest` | 7 |
| payment | `MomoPaymentStrategyTest` | 7 |
| payment | `PaymentGatewayPropertiesTest` | 3 |
| realtime | `StompOrderStatusPublisherTest` | 5 |
| demo | `DemoOrderSeederTest` | 4 |

Tổng cộng 163 test.

### 6.2. Kịch bản thử tay

Em thử trên ứng dụng chạy với cơ sở dữ liệu H2 trong bộ nhớ và bộ dữ liệu demo. Cột kết quả ghi đúng những gì quan sát được trên màn hình hoặc trong log.

*Bảng 6.2. Kịch bản thử tay*

| # | Thao tác | Kết quả mong đợi | Kết quả thật |
|---|---|---|---|
| 1 | Mở `/shop/register` khi chưa đăng nhập | Chuyển về trang đăng nhập | Chuyển về `/login` |
| 2 | Đăng ký shop mới với tên và địa chỉ hợp lệ | Tạo shop ở trạng thái PENDING, tài khoản vẫn là User | Tạo thành công, hiện thông báo chờ duyệt |
| 3 | Bỏ trống tên shop rồi gửi | Hiện lỗi ngay trên form | Hiện lỗi tại trường tên shop |
| 4 | Vendor đăng nhập rồi mở `/vendor/shop` của shop khác | Trả 403 | Trả về 403 như yêu cầu |
| 5 | Sửa thông tin shop hợp lệ | Lưu và hiện lại dữ liệu mới | Lưu thành công |
| 6 | Tải lên file `.txt` đổi tên thành `.png` | Báo lỗi định dạng ảnh | Báo lỗi tại ô chọn ảnh |
| 7 | Tải lên ảnh PNG hợp lệ | Upload lên Cloudinary và hiển thị ảnh | Đã hiển thị |
| 8 | Xoá sản phẩm chưa nằm trong đơn nào | Xoá thành công | Xoá thành công, sản phẩm biến khỏi danh sách |
| 9 | Vendor chuyển đơn từ CONFIRMED sang DELIVERED | Từ chối, báo không hợp lệ | Hiện thông báo "Không thể chuyển đơn từ Đã xác nhận sang Đã giao" |
| 10 | Mở `/vendor/products/{id}` với id của shop khác | Trả 403 | Trả về 403 như yêu cầu |
| 11 | Mở `/shipper/orders/{id}` với id của shipper khác | Trả 403 | Trả về 403 như yêu cầu |
| 12 | Shipper nhận đơn đang chờ | Đơn chuyển sang PICKED_UP | Nhận thành công, đơn biến khỏi danh sách chờ |
| 13 | Shipper giao thất bại và bỏ trống lý do | Từ chối, bắt buộc nhập lý do | Hiện lỗi yêu cầu nhập lý do |
| 14 | Shipper đánh dấu DELIVERED cho đơn COD | Đơn chuyển PAID tự động | Trạng thái thanh toán chuyển PAID |
| 15 | Mở hai tab, một tab của khách một tab của shop, rồi shop đổi trạng thái | Tab khách tự cập nhật không cần tải lại | Thành công |
| 16 | Gọi `/payment/vnpay/ipn` với chữ ký sai | Trả mã 97 kèm thông báo sai chữ ký | Trả `{"RspCode":"00","Message":"Confirm Success"}` khi cổng chưa cấu hình, xem mục 7.2 |
| 17 | Gọi `/payment/vnpay/return` với chữ ký sai | Về trang kết quả báo thất bại | Chuyển về `/payment/result` |

## 7. Khó khăn và cách xử lý

Ba vấn đề dưới đây là những lỗi em thực sự gặp khi chạy thử, không phải tình huống giả định.

### 7.1. Đơn hàng bị chuyển sai bước khi thao tác

**Triệu chứng.** Khi thử đổi trạng thái một đơn đang ở CONFIRMED sang DELIVERED, màn hình hiện thông báo "Không thể chuyển đơn từ Đã xác nhận sang Đã giao". Nếu chỉ có một controller tự kiểm tra thì rất dễ bỏ sót bước và đơn nhảy cóc qua các trạng thái trung gian.

**Nguyên nhân.** Ban đầu mỗi controller tự viết điều kiện `if` để kiểm tra trạng thái. Cách này lặp lại ở ba chỗ: màn hình Vendor, màn hình Shipper và chức năng huỷ đơn của khách. Chỉ cần một chỗ viết thiếu là đơn đi được bước không hợp lệ.

**Cách sửa.** Em tách luật ra lớp `OrderStatusTransitions` với hai bảng như trình bày ở mục 3.3.1, và đưa toàn bộ việc đổi trạng thái về `OrderStatusService.change`. Mọi controller chỉ gọi hàm này. Từ đó chỉ còn một nơi kiểm tra, thêm bước mới cũng chỉ sửa một chỗ.

### 7.2. IPN thanh toán báo thành công khi cổng chưa cấu hình

**Triệu chứng.** Em gọi thử `/payment/vnpay/ipn` với chữ ký sai và không có tham số. Kết quả trả về là `{"RspCode":"00","Message":"Confirm Success"}` với HTTP 200, tức là báo thành công. Trong log lại ghi dòng "VNPAY IPN tra ve that bai: Cong thanh toan VNPAY chua duoc cau hinh".

**Nguyên nhân.** Hàm `vnpayResponse` trong `PaymentIpnController` chỉ ánh xạ ba mã lỗi cụ thể là `CHU_KY_KHONG_HOP_LE`, `KHONG_TIM_THAY_DON`, `SAI_SO_TIEN`. Mọi trường hợp khác rơi vào nhánh `default` và nhánh này lại trả mã 00. Trường hợp cổng chưa cấu hình khoá thuộc nhánh này, nên bị báo thành công thay vì báo lỗi.

**Cách sửa.** Nhánh `default` phải trả một mã lỗi, không được trả 00. Theo tài liệu VNPAY, mã 99 dùng cho lỗi không xác định. Em ghi nhận đây là lỗi cần sửa và đưa vào hạn chế ở mục 8.2, chưa sửa trong lần chạy thử này vì thay đổi mã trả về ảnh hưởng tới cách cổng gọi lại nhiều lần.

Điều đáng chú ý là IPN chỉ cập nhật trạng thái thanh toán, không tạo ra đơn. Nhánh này rơi vào trước bước cập nhật nên dữ liệu đơn không bị sai, nhưng cổng sẽ hiểu nhầm là đã xử lý xong.

### 7.3. Xoá sản phẩm đã nằm trong đơn hàng

**Triệu chứng.** Em thử xoá một sản phẩm đã có trong đơn cũ. Hệ thống không cho xoá và hiện thông báo "Sản phẩm đã nằm trong đơn hàng nên không xoá được. Hãy dùng chức năng Tắt bán". Sản phẩm vẫn còn trong danh sách.

**Nguyên nhân.** Bảng chi tiết đơn hàng có khoá ngoại trỏ tới sản phẩm. Xoá cứng sản phẩm sẽ làm mất thông tin món trong các đơn cũ, và cơ sở dữ liệu sẽ chặn thao tác bằng lỗi ràng buộc khoá ngoại.

**Cách sửa.** Trước khi xoá, `VendorProductService.delete` kiểm tra sản phẩm đã được tham chiếu ở đâu chưa: đơn hàng, giỏ hàng, danh sách yêu thích hay đánh giá. Nếu có thì ném `IllegalStateException` kèm hướng dẫn dùng chức năng tắt bán. Nhờ vậy người dùng thấy thông báo rõ ràng thay vì lỗi 500 từ cơ sở dữ liệu.

## 8. Kết luận

### 8.1. Những gì đã hoàn thành

Em hoàn thành bảy nhóm chức năng theo bảng phân công:

*Bảng 8.1. Những gì đã hoàn thành theo phân công*

| Nhóm | Chức năng theo phân công | Trạng thái |
|---|---|---|
| Vendor | Đăng ký shop, quản lý trang chủ shop | Hoàn thành |
| Vendor | CRUD sản phẩm, size, topping, upload ảnh Cloudinary | Hoàn thành |
| Vendor | Quản lý đơn hàng theo 7 trạng thái | Hoàn thành |
| Vendor | Khuyến mãi, mã giảm giá của shop; báo cáo doanh thu, xuất CSV | Hoàn thành (khuyến mãi cùng bạn Quân) |
| Shipper | Nhận đơn, cập nhật trạng thái giao, thống kê đơn và tiền COD | Hoàn thành |
| Chung | Thanh toán VNPAY/MOMO sandbox, callback IPN, WebSocket đẩy trạng thái đơn | Hoàn thành, chưa chạy với khoá thật |

Ngoài danh sách trên, em viết thêm module dữ liệu demo để sinh đơn hàng mẫu.

### 8.2. Hạn chế còn lại

**Nhánh mặc định của IPN trả mã 00 sai.** Như trình bày ở mục 7.2, khi cổng chưa cấu hình hoặc gặp lỗi không nằm trong ba mã đã ánh xạ, IPN trả về `RspCode 00` thay vì một mã lỗi. Cần sửa nhánh `default` trong `vnpayResponse`.

**Chưa chạy với khoá sandbox thật.** VNPAY và MOMO mới thử ở mức dựng URL và kiểm chữ ký bằng test, chưa có giao dịch thật vì nhóm không có khoá.

**Không có bảng lịch sử trạng thái.** Lý do chuyển trạng thái hiện nối vào `Order.note`, giới hạn 500 ký tự, đủ để hiển thị nhưng không tra lại được ai đổi lúc nào. Muốn đúng cần thêm bảng `order_status_history`.

**Cấu hình WebSocket còn mở.** `WebSocketConfig` cho phép mọi origin; khi triển khai thật cần giới hạn về đúng tên miền của hệ thống.

**Khoá JWT trong file cấu hình dùng chung.** `application.properties` để khoá JWT mẫu cho demo; trước khi nộp và triển khai phải thay khoá và đưa ra biến môi trường.

### 8.3. Hướng phát triển

Bổ sung bảng `order_status_history` để lưu đủ lịch sử chuyển trạng thái, thêm thông báo đẩy cho shipper và tách cấu hình cổng thanh toán ra biến môi trường.
