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

- JDK 21 (ban LTS). May dang cai JDK 26 — Spring Boot 3.3 chua ho tro, nen cai them JDK 21 va tro `JAVA_HOME` vao do.
- Maven 3.9+
- Mot CSDL dang chay: SQL Server / MySQL / PostgreSQL

## Trang thai hien tai

Repo dang la **suon thu muc**. Buoc tiep theo (tuan 1):

- [ ] Quan: tao `pom.xml`, `application.properties`, entity + enum 5 vai (USER, VENDOR, MANAGER, ADMIN, SHIPPER), doi `Branch` thanh `Shop`
- [ ] Quan: layout `base.html` + chon template Bootstrap
- [ ] Hai: trang chu Guest
- [ ] Thien: dang ky shop
