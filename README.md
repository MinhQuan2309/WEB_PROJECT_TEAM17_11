# WEB_PROJECT_TEAM17_11 — UTETra

Do an mon Lap trinh Web — **De tai 11**: Website chuoi ban tra sua UTETra.
Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server/MySQL/PostgreSQL + JWT + WebSocket + Cloudinary.

## Thanh vien va phan cong

| Thanh vien | Khoi phu trach | Vai trong de tai |
|---|---|---|
| **Quan** | Nen tang chung + Quan tri | Chuc nang chung (auth/OTP/bao mat/tim kiem) + Manager + Admin |
| **Hai** | Luong mua hang | Guest + User |
| **Thien** | Nguoi ban + van chuyen + thanh toan | Vendor + Shipper |

Chi tiet day du: [docs/PHAN_CONG_NHIEM_VU.md](docs/PHAN_CONG_NHIEM_VU.md)

## Cay thu muc va chu so huu

Moi nguoi lam trong thu muc rieng cua minh de han che conflict khi merge.

```
src/main/java/com/utetra/
├── config/           [QUAN]   SecurityConfig, WebSocketConfig, CloudinaryConfig, MailConfig, DataSeeder
├── security/         [QUAN]   JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── common/           [QUAN]   BaseEntity, exception/, util/
├── entity/           [QUAN chu tri, ca nhom review]
├── repository/       [ai so huu entity thi viet repository]
└── module/
    ├── auth/         [QUAN]   dang ky, OTP email, dang nhap, quen mat khau
    ├── admin/        [QUAN]   user, danh muc, chiet khau, nha van chuyen
    ├── manager/      [QUAN]   quan tri theo pham vi shop
    ├── search/       [QUAN]   tim kiem & loc dung chung
    ├── media/        [QUAN]   CloudinaryService
    ├── catalog/      [HAI]    trang chu, danh muc, chi tiet san pham
    ├── cart/         [HAI]    gio hang luu DB
    ├── checkout/     [HAI]    dat hang, ap voucher
    ├── account/      [HAI]    profile, dia chi, wishlist, da xem
    ├── orderhistory/ [HAI]    lich su don theo trang thai
    ├── review/       [HAI]    danh gia + binh luan anh/video
    ├── vendor/       [THIEN]  dang ky shop, san pham, don, khuyen mai, doanh thu
    ├── shipper/      [THIEN]  don duoc giao, thong ke
    ├── payment/      [THIEN]  Strategy: COD / VNPAY / MOMO
    └── realtime/     [THIEN]  WebSocket day trang thai don

src/main/resources/
├── templates/layout/      [QUAN]  base.html (Decorator)  — dung chung
├── templates/fragments/   [QUAN]  navbar, footer, pagination — dung chung
├── templates/auth|admin|manager/          [QUAN]
├── templates/home|product|cart|checkout|account|orderhistory|review/   [HAI]
├── templates/vendor|shipper/              [THIEN]
└── static/css|js|img, static/template-bootstrap/  (template Bootstrap dung chung)
```

## Quy trinh Git

```
main       <- chi merge tu develop, luon chay duoc, dung de demo/bao ve
develop    <- nhanh tich hop, tat ca PR merge vao day
feat/<ten>/<module>   <- nhanh lam viec. VD: feat/quan/auth-otp, feat/hai/cart
```

Vong lam viec hang ngay:

```bash
git checkout develop
git pull origin develop
git checkout -b feat/quan/auth-otp
# ... code ...
git add .
git commit -m "feat(auth): gui OTP qua email khi dang ky"
git push -u origin feat/quan/auth-otp
# mo Pull Request vao develop, nho 1 nguoi con lai review
```

Luat nhom:

1. **Khong sua file trong thu muc cua nguoi khac.** Can doi thi nhan nhom chat.
2. File dung chung (`pom.xml`, `application.properties`, `layout/base.html`, `fragments/`, `SecurityConfig.java`, `entity/`) — ai sua phai bao nhom truoc, commit rieng mot commit nho, merge ngay trong ngay.
3. Khong push truc tiep vao `main`. Moi thu di qua PR vao `develop`.
4. Moi sang `git pull origin develop` ve nhanh minh.
5. **Khong commit** mat khau DB, JWT secret, key Cloudinary, key VNPAY/MOMO. Dung `application-local.properties` (da bo vao `.gitignore`).
6. Commit message: `feat|fix|docs|refactor(<module>): <mo ta ngan>`.

## Yeu cau moi truong

Ba nguoi khong can cai trung phien ban nhau. Du an da chot san:

| Thanh phan | Rang buoc | Vi sao khong lo lech phien ban |
|---|---|---|
| **JDK** | **17 tro len** (17, 21, 23, 26 deu chay) | `pom.xml` dat `java.version=17` nen Maven luon bien dich ra bytecode 17, bat ke may ai dang cai JDK nao |
| **Maven** | Khong can cai | Repo co san `mvnw` / `mvnw.cmd` (Maven Wrapper). Chay `./mvnw` la no tu tai dung Maven 3.9.11 |
| **Thu vien** | Da ghim trong `pom.xml` | Spring Boot 3.3.5 quan ly phien ban tat ca thu vien, ai build cung ra ban giong nhau |
| **CSDL** | SQL Server (mac dinh) | Bang tu sinh bang `ddl-auto=update`, moi nguoi tu tao database rong ten `utetra` tren may minh |
| **Mat khau DB** | Moi nguoi mot file rieng | `application-local.properties` khong commit nen khong ai de len cau hinh cua ai |

Cho khac nhau duy nhat phai tu lo: mat khau SQL Server cua may minh (va key Gmail/Cloudinary neu lam
den phan do). Tat ca nam trong `application-local.properties`.

Muon dung MySQL/PostgreSQL thay SQL Server thi bo comment driver trong `pom.xml` va khoi datasource
tuong ung trong `application.properties` — nhung ca nhom nen dung chung mot he CSDL cho do lech.

### Mo bang Spring Tools (STS)

1. `File` > `Import...` > `Maven` > **Existing Maven Projects** > tro vao thu muc `WEB_PROJECT_TEAM17_11`
   (dung `Import > General > Existing Projects`, vi nhu vay STS khong nhan ra la project Maven).
2. Doi STS tai thu vien lan dau (vai phut, thanh tien do goc duoi phai).
3. Kiem tra JDK: chuot phai project > `Properties` > `Java Build Path` > `Libraries`. Neu bao
   loi kieu "requires Java 17" thi vao `Window` > `Preferences` > `Java` > `Installed JREs`
   chon JDK 17 tro len.
4. Tao `application-local.properties` (xem muc duoi) roi chuot phai project >
   `Run As` > **Spring Boot App**.

Chay bang dong lenh thi khong can buoc 3:

```bash
./mvnw spring-boot:run
```

(Windows CMD/PowerShell dung `mvnw.cmd spring-boot:run`.)

## Chay lan dau

1. Tao database rong ten `utetra` trong SQL Server (cac bang se tu sinh, `ddl-auto=update`):

   ```sql
   CREATE DATABASE utetra COLLATE Vietnamese_CI_AS;
   ```

   > **Neu may ban da tao database tu truoc ngay 22/10**: phai XOA roi tao lai.
   > Luc do Hibernate con tao cot `varchar` (khong Unicode) nen ten tieng Viet
   > bi luu thanh dau hoi, vi du "Sinh to" thanh "Sinh t?". Tu commit
   > `fix(jpa): dung nvarchar` thi cot moi duoc tao kieu `nvarchar`.
   >
   > Mo SQL Server Management Studio va chay file
   > [`docs/sql/tao-lai-database.sql`](docs/sql/tao-lai-database.sql),
   > sau do chay lai app de `DataSeeder` tao lai du lieu mau.
   >
   > Muon kiem tra da dung chua thi chay tiep
   > [`docs/sql/kiem-tra-unicode.sql`](docs/sql/kiem-tra-unicode.sql).
2. Copy cau hinh rieng va dien mat khau:

   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```

   File `application-local.properties` nam trong `.gitignore` nen khong bi day len GitHub.
   Toi thieu phai dien `spring.datasource.username` va `spring.datasource.password`.
3. Chay:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Mo http://localhost:8080

Lan dau chay, `DataSeeder` tu tao du lieu mau (6 tai khoan, 2 shop, 4 danh muc, 12 san pham,
topping, 3 nha van chuyen). Xong roi doi `app.seed.enabled=false` trong `application.properties`.

### Tai khoan mau

Mat khau tat ca deu la `123456` (chi dung khi hoc).

| Tai khoan | Vai | Vao duoc |
|---|---|---|
| `admin` | ADMIN | `/admin` |
| `manager` | MANAGER | `/manager` |
| `vendor1`, `vendor2` | VENDOR | `/vendor` |
| `khach` | USER | `/account`, `/cart`, `/orders` |
| `shipper` | SHIPPER | `/shipper` |

Dang nhap duoc bang username hoac email. Sau khi dang nhap, `LoginSuccessHandler` tu dua moi vai
ve dung khu vuc cua minh.

### Kiem tra nhanh khong can CSDL

```bash
./mvnw test
```

Test chay tren H2 trong bo nho: kiem tra context len duoc, repository hop le, du lieu mau dung,
mat khau da BCrypt va tim kiem san pham chay.

## Trang thai hien tai

**Tuan 1 (phan cua Quan) da xong** — bo khung chay duoc, hai nguoi con lai bat dau duoc ngay:

- [x] `pom.xml` (Spring Boot 3.3.5, Thymeleaf + Layout Dialect, Security, JPA, Mail, WebSocket, JWT, Cloudinary)
- [x] `application.properties` + `application-local.properties.example` (khong commit mat khau)
- [x] Enum 5 vai `Role` (USER, VENDOR, MANAGER, ADMIN, SHIPPER) va 7 trang thai don `OrderStatus`
- [x] Mo hinh san nhieu shop: entity trung tam la `Shop` (khong con `Branch`)
- [x] 27 entity + `BaseEntity`, 20 repository (du toan bo ERD trong tai lieu phan cong)
- [x] `SecurityConfig`: phan quyen 5 vai, BCrypt, form login, JWT cho `/api/**`
- [x] `DataSeeder`: du lieu mau
- [x] Layout Bootstrap (`layout/base.html`) + fragment navbar / footer / pagination / product-card
- [x] Trang dang nhap, trang chu Guest (10 san pham ban chay), dashboard trong cho 4 khu vuc
- [x] `CloudinaryService` va `ProductSearchService` — hai diem giao dung chung
- [x] Test tren H2: 48 test, `./mvnw test` xanh
- [x] Da chay that voi SQL Server: 21 bang duoc tao, seed vao du lieu, trang chu ra 10 san pham,
      5 vai dang nhap vao dung khu vuc, khach bi chan khoi `/admin` (403)

### Diem giao san cho Hai va Thien

Ba interface/service dung chung da san sang, khong ai phai cho ai:

| Dung de | Goi nhu the nao | Ai viet |
|---|---|---|
| Upload anh/video | `cloudinaryService.upload(file, "products")` tra ve URL | Quan (xong) |
| Tim kiem & loc san pham | `productSearchService.search(filter, page, size)` voi `ProductFilter` | Quan (xong) |
| Thanh toan | `PaymentStrategy.createPayment(order)`, luu vao entity `Payment` | Thien (tuan 4) |
| Day trang thai don real-time | `OrderStatusPublisher.publish(order)` | Thien (tuan 4) |

Diem bat dau cua tung nguoi:

- **Hai**: `module/catalog/HomeController.java` (da co trang chu Guest) va `templates/home/index.html`.
  Lam tiep trang chu User 4 block 20 san pham, trang danh muc, chi tiet san pham, roi `cart` va `checkout`.
- **Thien**: `module/vendor/VendorHomeController.java` va `module/shipper/ShipperHomeController.java`
  (route da duoc phan quyen san). Lam tiep dang ky shop, CRUD san pham, don hang theo 7 trang thai.

### Kiem duyet shop (da xong)

Mat xich noi luong dang ky shop cua Thien voi khu /vendor:

- `/admin/shops` - Admin, pham vi toan san, them quyen phan cong nguoi quan ly
- `/manager/shops` - Manager, chi thay shop minh quan ly va shop chua ai nhan.
  Duyet mot shop chua ai nhan thi nhan luon shop do.

Vong doi trang thai:

```
PENDING  --duyet-->  ACTIVE  --tam dung-->  SUSPENDED --mo lai--> ACTIVE
   |
   +--tu choi--> REJECTED --mo lai ho so--> PENDING
```

Duyet shop la luc duy nhat chu shop duoc nang tu USER len VENDOR.

TODO ban giao cho Thien: them nut "gui lai ho so" ben /vendor de chu shop tu
chuyen REJECTED -> PENDING sau khi sua thong tin, khoi phai nho Admin mo ho.

### Con lai cua Quan

- [x] Dang ky + OTP email, kich hoat tai khoan, quen mat khau, dat lai mat khau (`module/auth`)
- [x] `POST /api/auth/login` tra JWT

#### Chay thu OTP khong can Gmail

Chua dien `spring.mail.username` trong `application-local.properties` thi he thong
KHONG gui mail that, ma in ma OTP ra Console cua ung dung:

```
================== OTP (CHE DO THU NGHIEM) ==================
Email nhan : nguoidung@example.com
MA OTP     : 965431
Het han sau: 5 phut
=============================================================
```

Nho vay ca nhom thu duoc luong dang ky va quen mat khau ngay. Khi nop bai thi dien
App Password cua Gmail vao la gui email that, khong phai sua code.
- [x] CRUD danh muc
- [x] Quan ly user, chiet khau app, khuyen mai san, nha van chuyen (`module/admin`)
- [x] Hoan thien khu Manager: dashboard, chu shop, san pham theo pham vi (`module/manager`)
- [x] CI tu chay test tren moi Pull Request (`.github/workflows/ci.yml`)

#### Pham vi cua Manager

De tai ghi Manager co cung danh sach chuc nang voi Admin nhung "quan ly tung cua hang".
Nhom chot cach hieu:

| Viec | Ai lam |
|---|---|
| Danh muc, nha van chuyen, khuyen mai cua san | chi Admin, vi la viec toan san |
| Shop, chu shop, san pham cua shop, chiet khau | Manager, trong pham vi shop minh phu trach |
| Khoa tai khoan, doi vai tro | chi Admin |

Manager nhan shop bang cach duyet mot shop dang cho ma chua ai nhan, hoac duoc Admin
phan cong o `/admin/shops`.

### Luu y ve quan he LAZY trong template

`spring.jpa.open-in-view=true` (mac dinh cua Spring Boot) nen trong Thymeleaf truy cap
`product.shop.name` khong bi `LazyInitializationException`. Doi thanh `false` la trang chu gay ngay.

Du vay, cho nao biet truoc se dung quan he nao thi van nen khai bao `@EntityGraph`
hoac `JOIN FETCH` trong repository cho do ton truy van, nhu
`ProductRepository.findTop10ByActiveTrueOrderBySoldCountDesc()` da lam.

### Luu y khi them entity moi

Tranh dat ten cot trung tu khoa SQL. Cot `value` cua `Voucher` da phai doi thanh
`discount_value` vi H2/SQL Server khong chiu. Gap loi "Syntax error in SQL statement ... create table"
thi kiem tra cho nay truoc.
