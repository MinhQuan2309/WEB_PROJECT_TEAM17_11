package com.utetra.module.vendor;

import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Product;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra bao cao doanh thu cua shop (tuan 5).
 */
@SpringBootTest
@Transactional
class VendorRevenueServiceTest {

    @Autowired VendorRevenueService revenueService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager entityManager;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    /**
     * createdAt duoc @PrePersist gan bang thoi diem hien tai, nen muon kiem tra bao cao
     * theo ngay/thang thi phai sua lai ngay tao don bang cau lenh SQL truc tiep.
     */
    private void datLaiNgayTao(Order order, java.time.LocalDateTime thoiDiem) {
        entityManager.createNativeQuery("update orders set created_at = :thoiDiem where id = :id")
                .setParameter("thoiDiem", thoiDiem)
                .setParameter("id", order.getId())
                .executeUpdate();
        order.setCreatedAt(thoiDiem);
    }

    private Order taoDon(Shop shop, OrderStatus status, String total, java.time.LocalDateTime thoiDiem) {
        Order order = new Order();
        order.setCode("REV-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        order.setSubtotal(new BigDecimal(total));
        order.setShippingFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal(total));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PAID);
        Order daLuu = orderRepository.save(order);
        datLaiNgayTao(daLuu, thoiDiem);
        return daLuu;
    }

    private void themMon(Order order, String tenMon, int soLuong, String tien) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductName(tenMon);
        item.setUnitPrice(new BigDecimal(tien));
        item.setQuantity(soLuong);
        item.setLineTotal(new BigDecimal(tien).multiply(BigDecimal.valueOf(soLuong)));
        orderItemRepository.save(item);
    }

    @Test
    @DisplayName("Chi tinh doanh thu cua don da giao, bo qua don dang giao va don huy")
    void chiTinhDonDaGiao() {
        Shop shop = shopOf("vendor1");
        taoDon(shop, OrderStatus.DELIVERED, "100000", java.time.LocalDateTime.now());
        taoDon(shop, OrderStatus.DELIVERED, "50000", java.time.LocalDateTime.now());
        taoDon(shop, OrderStatus.SHIPPING, "999000", java.time.LocalDateTime.now());
        taoDon(shop, OrderStatus.CANCELLED, "888000", java.time.LocalDateTime.now());

        var baoCao = revenueService.summary("vendor1", LocalDate.now(), LocalDate.now(),
                VendorRevenueService.GroupBy.DAY);

        assertEquals(0, new BigDecimal("150000").compareTo(baoCao.revenue()),
                "Chi hai don da giao duoc tinh");
        assertEquals(2, baoCao.deliveredCount());
        assertEquals(1, baoCao.cancelledCount(), "Mot don CANCELLED");
        assertEquals(4, baoCao.orderCount(), "Tat ca don trong ky, ke ca don chua giao");
        assertEquals(0, new BigDecimal("75000").compareTo(baoCao.averageOrderValue()));
    }

    @Test
    @DisplayName("Hoa hong theo ti le cua shop, thuc nhan = doanh thu - hoa hong")
    void hoaHongVaThucNhan() {
        Shop shop = shopOf("vendor1");
        shop.setCommissionRate(new BigDecimal("10.00"));
        shopRepository.save(shop);
        taoDon(shop, OrderStatus.DELIVERED, "200000", java.time.LocalDateTime.now());

        var baoCao = revenueService.summary("vendor1", LocalDate.now(), LocalDate.now(),
                VendorRevenueService.GroupBy.DAY);

        assertEquals(0, new BigDecimal("200000").compareTo(baoCao.revenue()));
        assertEquals(0, new BigDecimal("20000").compareTo(baoCao.commission()));
        assertEquals(0, new BigDecimal("180000").compareTo(baoCao.netRevenue()));
    }

    @Test
    @DisplayName("Doanh thu chi cua shop minh, khong lan sang shop khac")
    void khongLanSangShopKhac() {
        taoDon(shopOf("vendor1"), OrderStatus.DELIVERED, "100000", java.time.LocalDateTime.now());
        taoDon(shopOf("vendor2"), OrderStatus.DELIVERED, "7000000", java.time.LocalDateTime.now());

        var cua1 = revenueService.summary("vendor1", LocalDate.now(), LocalDate.now(),
                VendorRevenueService.GroupBy.DAY);

        assertEquals(0, new BigDecimal("100000").compareTo(cua1.revenue()));
        assertEquals(1, cua1.deliveredCount());
    }

    @Test
    @DisplayName("Bang doanh thu theo ngay: chi tiet dung ngay va dien ca ngay khong co don")
    void bangTheoNgay() {
        Shop shop = shopOf("vendor1");
        LocalDate homNay = LocalDate.now();
        taoDon(shop, OrderStatus.DELIVERED, "120000", homNay.atTime(10, 30));
        taoDon(shop, OrderStatus.DELIVERED, "80000", homNay.minusDays(2).atTime(9, 0));

        var baoCao = revenueService.summary("vendor1", homNay.minusDays(3), homNay,
                VendorRevenueService.GroupBy.DAY);

        assertEquals(4, baoCao.series().size(), "Phai dien du 4 ngay trong khoang");
        assertEquals(0, new BigDecimal("120000").compareTo(
                baoCao.series().get(3).revenue()), "Ngay cuoi la hom nay");
        assertEquals(1, baoCao.series().get(3).orders());
        assertEquals(0, baoCao.series().get(0).revenue().compareTo(BigDecimal.ZERO),
                "Ngay khong co don phai la 0 chu khong thieu dong");
    }

    @Test
    @DisplayName("Bang doanh thu theo thang: gop dung thang")
    void bangTheoThang() {
        Shop shop = shopOf("vendor1");
        LocalDate homNay = LocalDate.now();
        taoDon(shop, OrderStatus.DELIVERED, "100000", homNay.withDayOfMonth(1).atTime(8, 0));
        taoDon(shop, OrderStatus.DELIVERED, "50000", homNay.atTime(8, 0));

        var baoCao = revenueService.summary("vendor1", homNay.withDayOfMonth(1), homNay,
                VendorRevenueService.GroupBy.MONTH);

        assertEquals(1, baoCao.series().size(), "Trong cung mot thang thi chi co mot moc");
        assertEquals(0, new BigDecimal("150000").compareTo(baoCao.series().get(0).revenue()));
        assertEquals(2, baoCao.series().get(0).orders());
    }

    @Test
    @DisplayName("Top mon ban chay: gom theo ten mon, chi tinh don da giao")
    void topMonBanChay() {
        Shop shop = shopOf("vendor1");
        Order don1 = taoDon(shop, OrderStatus.DELIVERED, "100000", java.time.LocalDateTime.now());
        Order don2 = taoDon(shop, OrderStatus.DELIVERED, "100000", java.time.LocalDateTime.now());
        Order donHuy = taoDon(shop, OrderStatus.CANCELLED, "100000", java.time.LocalDateTime.now());

        themMon(don1, "Tra sua tran chau", 2, "30000");
        themMon(don2, "Tra sua tran chau", 1, "30000");
        themMon(don1, "Banh flan", 1, "20000");
        themMon(donHuy, "Mon Khong Tinh", 9, "30000");

        var baoCao = revenueService.summary("vendor1", LocalDate.now(), LocalDate.now(),
                VendorRevenueService.GroupBy.DAY);

        assertFalse(baoCao.topProducts().isEmpty());
        assertEquals("Tra sua tran chau", baoCao.topProducts().get(0).name(),
                "Mon ban nhieu nhat phai dung dau");
        assertEquals(3, baoCao.topProducts().get(0).quantity());
        assertTrue(baoCao.topProducts().stream().noneMatch(p -> p.name().equals("Mon Khong Tinh")),
                "Mon trong don da huy khong duoc tinh");
    }

    @Test
    @DisplayName("Khoang thoi gian sai (from > to) thi tu doi lai, khong nem loi")
    void khoangThoiGianNguocDuocDoiLai() {
        LocalDate homNay = LocalDate.now();
        var baoCao = revenueService.summary("vendor1", homNay, homNay.minusDays(5),
                VendorRevenueService.GroupBy.DAY);

        assertEquals(homNay.minusDays(5), baoCao.from());
        assertEquals(homNay, baoCao.to());
    }

    @Test
    @DisplayName("Khong truyen thoi gian thi mac dinh lay tu dau thang den hom nay")
    void macDinhDauThangDenHomNay() {
        var baoCao = revenueService.summary("vendor1", null, null, null);

        assertEquals(LocalDate.now().withDayOfMonth(1), baoCao.from());
        assertEquals(LocalDate.now(), baoCao.to());
        assertEquals(VendorRevenueService.GroupBy.DAY, baoCao.groupBy());
    }

    @Test
    @DisplayName("Khoang qua dai bi gioi han lai 366 ngay")
    void khoangQuaDaiBiGioiHan() {
        LocalDate homNay = LocalDate.now();
        var baoCao = revenueService.summary("vendor1", homNay.minusYears(5), homNay,
                VendorRevenueService.GroupBy.DAY);

        assertEquals(homNay.minusDays(365), baoCao.from());
    }

    @Test
    @DisplayName("Don khong nam trong khoang thoi gian thi khong duoc tinh")
    void ngoaiKhoangThoiGianThiBoQua() {
        Shop shop = shopOf("vendor1");
        LocalDate homNay = LocalDate.now();
        taoDon(shop, OrderStatus.DELIVERED, "500000", homNay.minusDays(40).atTime(10, 0));

        var baoCao = revenueService.summary("vendor1", homNay.minusDays(3), homNay,
                VendorRevenueService.GroupBy.DAY);

        assertEquals(0, BigDecimal.ZERO.compareTo(baoCao.revenue()));
        assertEquals(0, baoCao.deliveredCount());
    }

    @Test
    @DisplayName("San pham that trong du lieu mau khong lam bao cao loi")
    void baoCaoChayDuocTrenDuLieuThat() {
        Product product = productRepository
                .findByShopOrderByIdDesc(shopOf("vendor1"), PageRequest.of(0, 1))
                .getContent().get(0);
        assertTrue(product.getId() > 0);
        // Khong khang dinh so lieu cu the vi du lieu mau co the doi, chi can khong loi.
        revenueService.summary("vendor1", LocalDate.now().minusYears(1), LocalDate.now(),
                VendorRevenueService.GroupBy.MONTH);
    }
}