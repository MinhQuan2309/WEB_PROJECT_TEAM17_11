# WEB_PROJECT_TEAM17_11 — UTETra

Do an mon Lap trinh Web, **de tai 11**: website chuoi ban tra sua UTETra.

Spring Boot 3.3 · Thymeleaf · Bootstrap 5 · Spring Data JPA · SQL Server · Spring Security
· JWT · WebSocket (STOMP) · Cloudinary · VNPAY & MOMO sandbox.

## Nhom 17

| Thanh vien | Vai phu trach |
|---|---|
| Quan | Nen tang chung (entity, bao mat, auth + OTP, tim kiem, upload) + Manager + Admin |
| Hai | Guest + User: trang chu, danh muc, gio hang, thanh toan, lich su don, danh gia |
| Thien | Vendor + Shipper: shop, san pham, don hang, khuyen mai, doanh thu, thanh toan, real-time |

Chi tiet: [docs/PHAN_CONG_NHIEM_VU.md](docs/PHAN_CONG_NHIEM_VU.md)

---

## Chay lan dau

**Can co:** JDK 17 tro len · SQL Server dang chay. Khong can cai Maven, dung `mvnw` kem theo.

**1. Tao database**

```sql
CREATE DATABASE utetra COLLATE Vietnamese_CI_AS;
```

**2. Tao file cau hinh rieng** (da nam trong `.gitignore`, khong bi day len GitHub)

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Toi thieu phai dien `spring.datasource.username` va `spring.datasource.password`.

**3. Chay**

```bash
./mvnw spring-boot:run
```

Windows dung `mvnw.cmd spring-boot:run`. Mo http://localhost:8080 — lan dau chay se tu
tao bang va nap du lieu mau.

**4. Dang nhap thu.** Mat khau tat ca tai khoan mau deu la `123456`.

| Tai khoan | Vai | Vao duoc |
|---|---|---|
| `admin` | ADMIN | `/admin` |
| `manager` | MANAGER | `/manager` |
| `vendor1`, `vendor2` | VENDOR | `/vendor` |
| `khach` | USER | `/account`, `/cart`, `/orders` |
| `shipper` | SHIPPER | `/shipper` |

Dang nhap duoc bang username hoac email. Moi vai tu duoc dua ve dung khu cua minh.

### Mo bang Spring Tools

`File` > `Import...` > `Maven` > **Existing Maven Projects** > tro vao thu muc du an.
Dung dung `Import > General > Existing Projects`, kieu do STS khong nhan ra la project Maven.

Chay bang chuot phai project > `Run As` > **Spring Boot App**. Day la ung dung Spring Boot
co Tomcat nhung san, khong deploy vao Tomcat ngoai nen no khong hien trong tab Servers.

---

## Ba luu y quan trong

**Tieng Viet trong CSDL.** `application.properties` co dong
`hibernate.use_nationalized_character_data=true` de Hibernate tao cot `nvarchar`.
Thieu dong nay thi cot se la `varchar` va chu tieng Viet bi luu thanh dau hoi
("Sinh to" thanh "Sinh t?"). May nao da tao database tu truoc phai chay
[docs/sql/tao-lai-database.sql](docs/sql/tao-lai-database.sql) de tao lai,
roi [docs/sql/kiem-tra-unicode.sql](docs/sql/kiem-tra-unicode.sql) de xac nhan.

**Thu OTP khong can Gmail.** Chua dien `spring.mail.username` thi he thong khong gui mail
that ma in ma OTP ra Console:

```
================== OTP (CHE DO THU NGHIEM) ==================
Email nhan : nguoidung@example.com
MA OTP     : 965431
=============================================================
```

Dien App Password cua Gmail vao `application-local.properties` la gui that, khong sua code.

**Quan he LAZY trong template.** `spring.jpa.open-in-view=true` de Thymeleaf doc duoc
`product.shop.name` ma khong bi `LazyInitializationException`. Cho nao biet truoc se dung
quan he nao thi van nen khai bao `@EntityGraph` trong repository cho do ton truy van.

---

## Cau truc

```
src/main/java/com/utetra/
├── UtetraApplication.java
├── config/        WebSocketConfig, CloudinaryConfig, DataSeeder
├── security/      SecurityConfig, JwtService, JwtAuthenticationFilter, LoginSuccessHandler
├── common/        BaseEntity, util/SlugUtils
├── entity/        27 entity + cac enum (Role, OrderStatus, DiscountType...)
├── repository/    Cac JPA repository
└── module/        Chia theo nghiep vu, moi nguoi lam trong thu muc cua minh
```

| Module | Nguoi lam | Noi dung |
|---|---|---|
| `auth` | Quan | Dang ky + OTP email, kich hoat, quen mat khau, API login tra JWT |
| `admin` | Quan | Duyet shop, quan ly user, danh muc, nha van chuyen, khuyen mai san, chiet khau |
| `manager` | Quan | Shop, chu shop va san pham trong pham vi duoc phan cong |
| `search` | Quan | Tim kiem va loc san pham dung chung toan he thong |
| `media` | Quan | Upload anh/video len Cloudinary |
| `catalog` | Hai | Trang chu Guest/User, trang danh muc, chi tiet san pham |
| `cart` | Hai | Gio hang luu tren CSDL, nhom theo shop |
| `checkout` | Hai | Dat hang, ap ma giam gia, chon nha van chuyen |
| `account` | Hai | Ho so, doi mat khau, so dia chi, san pham yeu thich |
| `orderhistory` | Hai | Lich su don theo trang thai, huy don, tra hang |
| `review` | Hai | Danh gia 1-5 sao, binh luan >= 50 ky tu kem anh/video |
| `vendor` | Thien | Dang ky shop, san pham, topping, don hang, khuyen mai, doanh thu |
| `shipper` | Thien | Nhan don, cap nhat trang thai giao, thong ke COD |
| `payment` | Thien | COD / VNPAY / MOMO sandbox, callback IPN |
| `realtime` | Thien | Day trang thai don qua WebSocket |
| `orderstatus` | Thien | May trang thai dung chung cho 7 trang thai don hang |
| `demo` | Thien | Sinh don hang mau phuc vu buoi demo |

Giao dien dat trong `src/main/resources/templates/<ten module>/`, dung chung layout
`layout/base.html` (Decorator pattern) va cac fragment trong `fragments/`.

### Pham vi cua Manager

De tai ghi Manager co cung danh sach chuc nang voi Admin nhung "quan ly tung cua hang".
Nhom chot cach hieu:

| Viec | Ai lam |
|---|---|
| Danh muc, nha van chuyen, khuyen mai cua san | chi Admin, vi la viec toan san |
| Shop, chu shop, san pham cua shop, chiet khau | Manager, trong pham vi shop minh phu trach |
| Khoa tai khoan, doi vai tro | chi Admin |

Manager nhan shop bang cach duyet mot shop dang cho ma chua ai nhan, hoac duoc Admin
phan cong o `/admin/shops`.

---

## Quy trinh Git

```
main       <- ban luon chay duoc, dung de demo va bao ve
develop    <- nhanh tich hop, moi PR merge vao day
feat/<ten>/<viec>   <- nhanh lam viec
```

```bash
git checkout develop
git pull origin develop
git checkout -b feat/quan/ten-viec
# ... code ...
git add .
git commit -m "feat(auth): mo ta ngan"
git push -u origin feat/quan/ten-viec
```

Mo Pull Request vao `develop` (khong phai `main`), nho mot nguoi khac review roi merge.
Merge xong thi xoa nhanh.

**Luat nhom**

1. Khong sua file trong thu muc module cua nguoi khac. Can doi thi bao nhom.
2. File dung chung (`pom.xml`, `application.properties`, `entity/`, `repository/`,
   `SecurityConfig`, `layout/`, `fragments/`) sua thi bao truoc, commit rieng mot commit nho.
3. Khong push thang vao `main`.
4. **Chay `./mvnw test` truoc khi push.** CI cung chay lai tren moi PR, PR hong hien dau X do.
5. Khong commit mat khau hay API key. Tat ca de trong `application-local.properties`.
6. Dung bao gio chep de ca thu muc du an len thu muc dang co `.git` — Git se ghi nhan
   thanh xoa file cua nguoi khac.

---

## Kiem thu

```bash
./mvnw test
```

Test chay tren H2 trong bo nho nen khong can SQL Server dang bat.

---

## Tai lieu

| File | Noi dung |
|---|---|
| [docs/PHAN_CONG_NHIEM_VU.md](docs/PHAN_CONG_NHIEM_VU.md) | Phan cong chi tiet, so do phu thuoc giua ba khoi |
| [docs/KhaoSat_PhanTichHeThong.docx](docs/KhaoSat_PhanTichHeThong.docx) | Khao sat hien trang va phan tich thiet ke |
| [docs/sql/](docs/sql/) | Script tao lai database va kiem tra unicode |
| [docs/BaoCao/](docs/BaoCao/) | Bao cao cua tung thanh vien |
