package com.utetra.config;

import com.utetra.entity.*;
import com.utetra.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tao du lieu mau de ca nhom co cai ma chay thu ngay.
 * Chi chay khi app.seed.enabled=true VA bang users dang trong.
 *
 * Mat khau tat ca tai khoan mau: 123456 (chi dung khi hoc, khong dung that).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String RAW_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ToppingRepository toppingRepository;
    private final CarrierRepository carrierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ShopRepository shopRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      ProductVariantRepository variantRepository,
                      ToppingRepository toppingRepository,
                      CarrierRepository carrierRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.toppingRepository = toppingRepository;
        this.carrierRepository = carrierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Da co du lieu, bo qua buoc tao du lieu mau.");
            return;
        }

        log.info("Dang tao du lieu mau cho UTETra...");

        User admin = user("admin", "Nguyen Van Admin", "admin@utetra.vn", Role.ADMIN);
        User manager = user("manager", "Tran Thi Manager", "manager@utetra.vn", Role.MANAGER);
        User vendor1 = user("vendor1", "Le Van Chu Shop 1", "vendor1@utetra.vn", Role.VENDOR);
        User vendor2 = user("vendor2", "Pham Thi Chu Shop 2", "vendor2@utetra.vn", Role.VENDOR);
        User customer = user("khach", "Hoang Van Khach", "khach@utetra.vn", Role.USER);
        User shipper = user("shipper", "Vo Van Shipper", "shipper@utetra.vn", Role.SHIPPER);
        userRepository.saveAll(List.of(admin, manager, vendor1, vendor2, customer, shipper));

        Shop shop1 = shop(vendor1, manager, "UTETra Le Van Viet",
                "Chi nhanh Le Van Viet, Thu Duc", "207 Le Van Viet, Thu Duc, TP.HCM");
        Shop shop2 = shop(vendor2, manager, "UTETra Vo Van Ngan",
                "Chi nhanh Vo Van Ngan, Thu Duc", "01 Vo Van Ngan, Thu Duc, TP.HCM");
        shopRepository.saveAll(List.of(shop1, shop2));

        Category traSua = category("Tra sua", "tra-sua", null);
        Category traTraiCay = category("Tra trai cay", "tra-trai-cay", null);
        Category caPhe = category("Ca phe", "ca-phe", null);
        Category doAnKem = category("Do an kem", "do-an-kem", null);
        categoryRepository.saveAll(List.of(traSua, traTraiCay, caPhe, doAnKem));

        createProduct(shop1, traSua, "Tra sua tran chau duong den", 35000, 120, 48, 5);
        createProduct(shop1, traSua, "Tra sua Oolong kem pho mai", 42000, 95, 31, 5);
        createProduct(shop1, traSua, "Tra sua matcha", 40000, 80, 22, 4);
        createProduct(shop1, traTraiCay, "Tra dao cam sa", 38000, 150, 65, 5);
        createProduct(shop1, traTraiCay, "Tra vai", 36000, 70, 18, 4);
        createProduct(shop1, caPhe, "Ca phe sua da", 25000, 60, 12, 4);
        createProduct(shop2, traSua, "Tra sua truyen thong", 30000, 200, 88, 5);
        createProduct(shop2, traSua, "Tra sua khoai mon", 38000, 110, 26, 4);
        createProduct(shop2, traTraiCay, "Tra oi hong", 39000, 90, 24, 5);
        createProduct(shop2, traTraiCay, "Tra chanh day", 32000, 130, 41, 4);
        createProduct(shop2, caPhe, "Bac xiu", 29000, 55, 9, 4);
        createProduct(shop2, doAnKem, "Banh flan tran chau", 20000, 45, 15, 5);

        for (Shop s : List.of(shop1, shop2)) {
            toppingRepository.saveAll(List.of(
                    topping(s, "Tran chau den", 5000),
                    topping(s, "Tran chau trang", 5000),
                    topping(s, "Thach dua", 6000),
                    topping(s, "Pudding trung", 8000),
                    topping(s, "Kem pho mai", 10000)));
        }

        carrierRepository.saveAll(List.of(
                carrier("Giao hang trong 30 phut", 15000),
                carrier("Giao hang tieu chuan", 20000),
                carrier("Tu den lay", 0)));

        log.info("Xong. Tai khoan mau (mat khau {}): admin / manager / vendor1 / vendor2 / khach / shipper",
                RAW_PASSWORD);
    }

    private User user(String username, String fullName, String email, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPhone("09000000" + (username.length() % 10) + "0");
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    private Shop shop(User owner, User manager, String name, String description, String address) {
        Shop s = new Shop();
        s.setOwner(owner);
        s.setManager(manager);
        s.setName(name);
        s.setDescription(description);
        s.setAddress(address);
        s.setPhone("02838960000");
        s.setStatus(ShopStatus.ACTIVE);
        s.setCommissionRate(new BigDecimal("10.00"));
        return s;
    }

    private Category category(String name, String slug, Category parent) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setParent(parent);
        return c;
    }

    private void createProduct(Shop shop, Category category, String name,
                               long price, int sold, int likes, int rating) {
        Product p = new Product();
        p.setShop(shop);
        p.setCategory(category);
        p.setName(name);
        p.setDescription(name + " cua " + shop.getName()
                + ". Pha tu nguyen lieu chon loc, do ngot va da dieu chinh theo yeu cau.");
        p.setBasePrice(BigDecimal.valueOf(price));
        p.setSoldCount(sold);
        p.setLikeCount(likes);
        p.setRatingAvg(BigDecimal.valueOf(rating));
        p.setRatingCount(Math.max(sold / 10, 1));
        productRepository.save(p);

        variantRepository.saveAll(List.of(
                variant(p, "Size S", 0, 100),
                variant(p, "Size M", 5000, 100),
                variant(p, "Size L", 10000, 100)));
    }

    private ProductVariant variant(Product product, String name, long extra, int stock) {
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setName(name);
        v.setExtraPrice(BigDecimal.valueOf(extra));
        v.setStock(stock);
        return v;
    }

    private Topping topping(Shop shop, String name, long price) {
        Topping t = new Topping();
        t.setShop(shop);
        t.setName(name);
        t.setPrice(BigDecimal.valueOf(price));
        return t;
    }

    private Carrier carrier(String name, long fee) {
        Carrier c = new Carrier();
        c.setName(name);
        c.setFee(BigDecimal.valueOf(fee));
        return c;
    }
}
