package com.utetra.module.demo;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.PaymentRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra du lieu demo (tuan 6): tao don hang trai deu nhieu ngay, du trang thai de
 * cac man hinh Vendor / Shipper / Doanh thu co du lieu, va chay lai thi khong nhan doi.
 */
@SpringBootTest(properties = "app.demo.orders.enabled=true")
@Transactional
class DemoOrderSeederTest {

    @Autowired DemoOrderSeeder demoOrderSeeder;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    @Test
    @DisplayName("Tao duoc don hang demo cho ca hai shop voi nhieu trang thai khac nhau")
    void taoDuocDonHangDemo() {
        List<Order> tatCa = orderRepository.findAll();
        assertFalse(tatCa.isEmpty(), "Phai co don hang demo");

        Shop shop1 = shopRepository.findByOwner(userRepository.findByUsername("vendor1").orElseThrow())
                .orElseThrow();
        Shop shop2 = shopRepository.findByOwner(userRepository.findByUsername("vendor2").orElseThrow())
                .orElseThrow();

        assertTrue(tatCa.stream().anyMatch(o -> o.getShop().getId().equals(shop1.getId())));
        assertTrue(tatCa.stream().anyMatch(o -> o.getShop().getId().equals(shop2.getId())));

        // Man hinh Vendor can don moi, shipper can don cho nhan va don dang giao,
        // trang doanh thu can don da giao.
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.NEW));
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.DELIVERED));
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.SHIPPING
                || o.getStatus() == OrderStatus.PICKED_UP));
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.RETURNED_REFUNDED));
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.CONFIRMED
                        && o.getShipper() == null),
                "Phai co don da xac nhan chua co shipper de thu muc 'Don cho nhan'");
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.DELIVERED
                        && o.getPaymentStatus() == PaymentStatus.PAID));
        assertTrue(tatCa.stream().anyMatch(o -> o.getStatus() == OrderStatus.DELIVERED
                        && o.getPaymentMethod() == com.utetra.entity.PaymentMethod.COD),
                "Phai co don COD da giao de thong ke tien thu ho");
    }

    @Test
    @DisplayName("Don da giao co ban ghi giao hang va thanh toan, moi don deu co mon")
    void donDaGiaoCoDuBanGhiLienQuan() {
        List<Order> daGiao = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .toList();
        assertFalse(daGiao.isEmpty());

        for (Order order : daGiao) {
            assertNotNull(order.getItems());
            assertFalse(order.getItems().isEmpty(), "Don " + order.getCode() + " phai co mon");
            assertTrue(shipmentRepository.findByOrder(order).isPresent(),
                    "Don " + order.getCode() + " phai co ban ghi giao hang");
            assertFalse(paymentRepository.findByOrder(order).isEmpty(),
                    "Don " + order.getCode() + " phai co ban ghi thanh toan");
        }
    }

    @Test
    @DisplayName("Don hang demo trai deu nhieu ngay trong 30 ngay gan nhat")
    void donHangTraiDeuTheoNgay() {
        LocalDate homNay = LocalDate.now();
        List<LocalDate> ngay = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null)
                .map(o -> o.getCreatedAt().toLocalDate())
                .toList();

        assertTrue(ngay.stream().allMatch(d -> !d.isAfter(homNay)), "Khong co don o tuong lai");
        assertTrue(ngay.stream().allMatch(d -> d.isAfter(homNay.minusDays(31))),
                "Don demo nam trong khoang 30 ngay gan nhat");
        assertTrue(ngay.stream().distinct().count() >= 5,
                "Don phai rai ra nhieu ngay thi bieu do doanh thu moi co dang");
    }

    @Test
    @DisplayName("Chay lai seeder khong tao trung don hang")
    void chayLaiKhongTaoTrung() {
        long truoc = orderRepository.count();
        long thanhToanTruoc = paymentRepository.count();

        demoOrderSeeder.taoDuLieuDemo();

        assertEquals(truoc, orderRepository.count(), "Chay lai khong duoc nhan doi don hang");
        assertEquals(thanhToanTruoc, paymentRepository.count());
    }
}