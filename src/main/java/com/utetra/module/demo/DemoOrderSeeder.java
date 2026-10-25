package com.utetra.module.demo;

import com.utetra.entity.Carrier;
import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Payment;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Product;
import com.utetra.entity.ProductVariant;
import com.utetra.entity.Role;
import com.utetra.entity.Shipment;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.PaymentRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Du lieu demo cho phan cua Thien (tuan 6): don hang, giao hang, thanh toan.
 *
 * Vi sao tach rieng khoi DataSeeder dung chung: DataSeeder chi tao nguoi dung, shop,
 * san pham. Cac man hinh Don hang / Doanh thu / Shipper / Thong ke COD can co don hang
 * that trai deu vai tuan thi moi nhin thay bieu do. De khong dung vao file dung chung,
 * phan du lieu nay nam o day va bat/tat bang cau hinh rieng:
 *
 *   app.seed.enabled=true          (da co san: tao nguoi dung, shop, san pham)
 *   app.demo.orders.enabled=true   (bat them: tao don hang demo)
 *
 * Mac dinh TAT de khong lam lech so lieu trong test tu dong.
 * Chay lai nhieu lan cung duoc: thay da co don hang thi bo qua.
 */
@Component
@ConditionalOnProperty(name = "app.demo.orders.enabled", havingValue = "true")
public class DemoOrderSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoOrderSeeder.class);

    private static final int SO_NGAY = 30;
    private static final BigDecimal PHI_GIAO = BigDecimal.valueOf(15000);
    /** Co dinh hat giong de moi lan tao du lieu demo deu giong nhau. */
    private static final long HAT_GIONG = 20261019L;
    /**
     * Tai khoan khach danh rieng cho du lieu demo. Co y KHONG dung "khach" vi do la
     * tai khoan goc ma test tu dong cua nhom dua vao.
     */
    private static final String TAI_KHOAN_KHACH_DEMO = "demo1";

    private static final String[] TEN_KHACH = {
            "Nguyen Minh Anh", "Tran Quoc Bao", "Le Thu Ha", "Pham Gia Huy",
            "Hoang Ngoc Lan", "Vo Duc Manh", "Dang Thanh Nga", "Bui Tuan Kiet"};
    private static final String[] DIA_CHI = {
            "12 Le Van Viet, Thu Duc, TP.HCM", "45 Vo Van Ngan, Thu Duc, TP.HCM",
            "78 Do Xuan Hop, Thu Duc, TP.HCM", "3 Pham Van Dong, Thu Duc, TP.HCM",
            "156 To Ngoc Van, Thu Duc, TP.HCM", "89 Kha Van Can, Thu Duc, TP.HCM"};

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final com.utetra.repository.CarrierRepository carrierRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public DemoOrderSeeder(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           PaymentRepository paymentRepository,
                           ShipmentRepository shipmentRepository,
                           ShopRepository shopRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository variantRepository,
                           UserRepository userRepository,
                           com.utetra.repository.CarrierRepository carrierRepository,
                           PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.carrierRepository = carrierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Chay sau khi ung dung khoi dong xong (ApplicationReadyEvent) de chac chan
     * DataSeeder dung chung da tao xong nguoi dung, shop va san pham truoc do,
     * khong phu thuoc vao thu tu dang ky ApplicationRunner.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void taoDuLieuDemo() {
        if (orderRepository.count() > 0) {
            log.info("Da co don hang, bo qua buoc tao don hang demo.");
            return;
        }
        User shipper = userRepository.findByUsername("shipper").orElse(null);
        User vendor1 = userRepository.findByUsername("vendor1").orElse(null);
        User vendor2 = userRepository.findByUsername("vendor2").orElse(null);
        if (shipper == null || vendor1 == null || vendor2 == null) {
            log.warn("Chua co du lieu nen tang (vendor1/vendor2/shipper), bo qua don hang demo.");
            return;
        }

        // Khach cua don hang demo la tai khoan RIENG (demo1..demo5), khong dung "khach".
        // Ly do: "khach" la tai khoan ma test tu dong cua nhom dung lam diem xuat phat.
        // Neu don demo DELIVERED gan vao "khach" thi test kiem tra "chua mua thi khong
        // duoc danh gia" se that bai vi khach da co san don da giao.
        User khach = layHoacTaoKhachDemo();

        Shop shop1 = shopRepository.findByOwner(vendor1).orElse(null);
        Shop shop2 = shopRepository.findByOwner(vendor2).orElse(null);
        if (shop1 == null || shop2 == null) {
            log.warn("Chua co shop nao, bo qua don hang demo.");
            return;
        }

        log.info("Dang tao don hang demo cho do an UTETra...");
        Random random = new Random(HAT_GIONG);
        Carrier carrier = carrierRepository.findAll().stream().findFirst().orElse(null);
        int dem = 0;

        dem += taoLoDon(shop1, khach, shipper, carrier, random, 18, "DH1");
        dem += taoLoDon(shop2, khach, shipper, carrier, random, 7, "DH2");

        // Vai don moi de man hinh Vendor co muc "Don moi" va shipper co don cho nhan.
        taoDon(shop1, khach, null, carrier, random, OrderStatus.NEW, LocalDateTime.now().minusHours(2), "DH1M1");
        taoDon(shop1, khach, null, carrier, random, OrderStatus.NEW, LocalDateTime.now().minusHours(1), "DH1M2");
        taoDon(shop1, khach, null, carrier, random, OrderStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(40), "DH1C1");
        taoDon(shop2, khach, null, carrier, random, OrderStatus.NEW,
                LocalDateTime.now().minusMinutes(30), "DH2M1");
        dem += 4;

        log.info("Da tao {} don hang demo (trai deu {} ngay gan nhat).", dem, SO_NGAY);
    }

    /**
     * Lay tai khoan khach danh rieng cho du lieu demo ("demo1"), tao moi neu chua co.
     *
     * Tach khoi tai khoan "khach" de du lieu demo khong lam sai lech cac test tu dong
     * cua nhom (vi du test danh gia san pham can khach CHUA mua hang).
     */
    private User layHoacTaoKhachDemo() {
        return userRepository.findByUsername(TAI_KHOAN_KHACH_DEMO).orElseGet(() -> {
            User u = new User();
            u.setUsername(TAI_KHOAN_KHACH_DEMO);
            u.setFullName("Khach Demo UTETra");
            u.setEmail("demo1@utetra.vn");
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRole(Role.USER);
            u.setEnabled(true);
            User daLuu = userRepository.save(u);
            log.info("Da tao tai khoan khach demo '{}' (mat khau 123456).", TAI_KHOAN_KHACH_DEMO);
            return daLuu;
        });
    }

    /** Tao mot lo don trai deu trong SO_NGAY ngay gan nhat, trang thai tron lan co chu y. */
    private int taoLoDon(Shop shop, User khach, User shipper, Carrier carrier,
                         Random random, int soDon, String tienToMa) {
        LocalDate homNay = LocalDate.now();
        int dem = 0;
        for (int i = 0; i < soDon; i++) {
            LocalDate ngay = homNay.minusDays(random.nextInt(SO_NGAY));
            LocalDateTime thoiDiem = ngay.atTime(9 + random.nextInt(11), random.nextInt(60));
            OrderStatus trangThai = chonTrangThai(random);
            User nguoiGiao = switch (trangThai) {
                case DELIVERED, SHIPPING, PICKED_UP, RETURNED_REFUNDED -> shipper;
                default -> null;
            };
            taoDon(shop, khach, nguoiGiao, carrier, random, trangThai, thoiDiem,
                    tienToMa + (i + 1));
            dem++;
        }
        return dem;
    }

    private OrderStatus chonTrangThai(Random random) {
        int so = random.nextInt(100);
        if (so < 68) {
            return OrderStatus.DELIVERED;
        }
        if (so < 78) {
            return OrderStatus.SHIPPING;
        }
        if (so < 84) {
            return OrderStatus.PICKED_UP;
        }
        if (so < 90) {
            return OrderStatus.CONFIRMED;
        }
        if (so < 96) {
            return OrderStatus.CANCELLED;
        }
        return OrderStatus.RETURNED_REFUNDED;
    }

    private Order taoDon(Shop shop, User khach, User shipper, Carrier carrier, Random random,
                         OrderStatus trangThai, LocalDateTime thoiDiem, String maDon) {
        List<Product> sanPham = productRepository
                .findByShopAndActiveOrderByIdDesc(shop, true,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();
        if (sanPham.isEmpty()) {
            sanPham = productRepository
                    .findByShopOrderByIdDesc(shop, org.springframework.data.domain.PageRequest.of(0, 20))
                    .getContent();
        }
        if (sanPham.isEmpty()) {
            return null;
        }

        String tenKhach = TEN_KHACH[random.nextInt(TEN_KHACH.length)];
        Order order = new Order();
        order.setCode(maDon + "-" + thoiDiem.toLocalDate());
        order.setCustomer(khach);
        order.setShop(shop);
        order.setShipper(shipper);
        order.setCarrier(carrier);
        order.setReceiverName(tenKhach);
        order.setReceiverPhone("09" + (10000000 + random.nextInt(89999999)));
        order.setShippingAddress(DIA_CHI[random.nextInt(DIA_CHI.length)]);
        order.setShippingFee(PHI_GIAO);
        order.setDiscount(BigDecimal.ZERO);
        order.setStatus(trangThai);
        order.setPaymentMethod(random.nextInt(100) < 70 ? PaymentMethod.COD : PaymentMethod.VNPAY);
        order.setPaymentStatus(trangThai == OrderStatus.DELIVERED
                ? PaymentStatus.PAID : PaymentStatus.PENDING);

        BigDecimal tamTinh = BigDecimal.ZERO;
        List<OrderItem> mon = new ArrayList<>();
        int soMon = 1 + random.nextInt(3);
        for (int i = 0; i < soMon; i++) {
            Product product = sanPham.get(random.nextInt(sanPham.size()));
            List<ProductVariant> variants = variantRepository.findByProductOrderByIdAsc(product);
            ProductVariant variant = variants.isEmpty() ? null
                    : variants.get(random.nextInt(variants.size()));
            BigDecimal donGia = product.getBasePrice() == null ? BigDecimal.ZERO : product.getBasePrice();
            if (variant != null && variant.getExtraPrice() != null) {
                donGia = donGia.add(variant.getExtraPrice());
            }
            int soLuong = 1 + random.nextInt(2);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setVariantName(variant == null ? null : variant.getName());
            item.setUnitPrice(donGia);
            item.setQuantity(soLuong);
            item.setLineTotal(donGia.multiply(BigDecimal.valueOf(soLuong)));
            mon.add(item);
            tamTinh = tamTinh.add(item.getLineTotal());
        }
        order.setSubtotal(tamTinh);
        order.setTotal(tamTinh.add(PHI_GIAO));

        Order daLuu = orderRepository.save(order);
        for (OrderItem item : mon) {
            item.setOrder(daLuu);
            orderItemRepository.save(item);
        }
        daLuu.setItems(mon);

        datLaiNgayTao(daLuu, thoiDiem);
        if (nguoiGiaoDuoc(trangThai)) {
            taoBanGhiGiaoHang(daLuu, shipper, carrier, thoiDiem, trangThai);
        }
        if (daLuu.getPaymentStatus() == PaymentStatus.PAID) {
            taoBanGhiThanhToan(daLuu, thoiDiem);
        }
        return daLuu;
    }

    private boolean nguoiGiaoDuoc(OrderStatus trangThai) {
        return trangThai != OrderStatus.NEW && trangThai != OrderStatus.CONFIRMED
                && trangThai != OrderStatus.CANCELLED;
    }

    private void taoBanGhiGiaoHang(Order order, User shipper, Carrier carrier,
                                   LocalDateTime thoiDiem, OrderStatus trangThai) {
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipper(shipper);
        shipment.setCarrier(carrier);
        shipment.setTrackingCode("TRK" + order.getCode());
        shipment.setFee(order.getShippingFee());
        shipment.setAssignedAt(thoiDiem.plusMinutes(10));
        if (trangThai == OrderStatus.SHIPPING || trangThai == OrderStatus.DELIVERED
                || trangThai == OrderStatus.RETURNED_REFUNDED) {
            shipment.setPickedUpAt(thoiDiem.plusMinutes(25));
        }
        if (trangThai == OrderStatus.DELIVERED) {
            shipment.setDeliveredAt(thoiDiem.plusMinutes(45));
        }
        if (trangThai == OrderStatus.RETURNED_REFUNDED) {
            shipment.setFailedReason("Khach khong nghe may, giao lai 2 lan khong duoc");
        }
        shipmentRepository.save(shipment);
    }

    private void taoBanGhiThanhToan(Order order, LocalDateTime thoiDiem) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(order.getPaymentMethod());
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(order.getTotal());
        payment.setTransactionId(order.getPaymentMethod() == PaymentMethod.COD
                ? "COD-" + order.getCode()
                : "GW-" + Math.abs(order.getCode().hashCode()));
        payment.setGatewayResponse("Du lieu demo tao san");
        payment.setPaidAt(thoiDiem.plusMinutes(50));
        paymentRepository.save(payment);
    }

    /**
     * BaseEntity tu gan createdAt khi luu, nen muon don hang trai deu cac ngay thi phai
     * sua lai bang cau lenh SQL truc tiep.
     */
    private void datLaiNgayTao(Order order, LocalDateTime thoiDiem) {
        entityManager.createNativeQuery("update orders set created_at = :thoiDiem where id = :id")
                .setParameter("thoiDiem", thoiDiem)
                .setParameter("id", order.getId())
                .executeUpdate();
        order.setCreatedAt(thoiDiem);
    }
}