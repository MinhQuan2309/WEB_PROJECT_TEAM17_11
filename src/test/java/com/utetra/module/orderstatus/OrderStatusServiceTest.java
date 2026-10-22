package com.utetra.module.orderstatus;

import com.utetra.entity.Carrier;
import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shipment;
import com.utetra.entity.Shop;
import com.utetra.repository.CarrierRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra OrderStatusService (tuan 3): cac tac dong kem theo khi doi trang thai.
 * Phan nay dung chung cho Vendor (tuan 3) va Shipper (tuan 5) nen duoc kiem tra rieng.
 */
@SpringBootTest
@Transactional
class OrderStatusServiceTest {

    @Autowired OrderStatusService orderStatusService;
    @Autowired OrderRepository orderRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired CarrierRepository carrierRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    private Order taoDon(OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setCode("STS-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shopOf("vendor1"));
        order.setShipper(userRepository.findByUsername("shipper").orElseThrow());
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        order.setSubtotal(BigDecimal.valueOf(50000));
        order.setShippingFee(BigDecimal.valueOf(15000));
        order.setTotal(BigDecimal.valueOf(65000));
        order.setStatus(status);
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    private Shipment taoShipment(Order order) {
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipper(order.getShipper());
        Carrier carrier = carrierRepository.findAll().get(0);
        shipment.setCarrier(carrier);
        shipment.setFee(BigDecimal.valueOf(15000));
        shipment.setAssignedAt(LocalDateTime.now());
        return shipmentRepository.save(shipment);
    }

    @Test
    @DisplayName("Shipper danh dau da giao: don COD chuyen sang da thanh toan, ghi moc giao hang")
    void shipperGiaoXongThiDaThanhToan() {
        Order order = taoDon(OrderStatus.SHIPPING, PaymentMethod.COD, PaymentStatus.PENDING);
        Shipment shipment = taoShipment(order);

        Order sau = orderStatusService.change(order, OrderActor.SHIPPER, OrderStatus.DELIVERED, "Giao thanh cong");

        assertEquals(OrderStatus.DELIVERED, sau.getStatus());
        assertEquals(PaymentStatus.PAID, sau.getPaymentStatus());
        assertNotNull(shipmentRepository.findById(shipment.getId()).orElseThrow().getDeliveredAt());
        assertTrue(sau.getNote().contains("Giao thanh cong"));
    }

    @Test
    @DisplayName("Don VNPAY da tra tien thi khi giao xong khong bi doi trang thai thanh toan")
    void donVnpayDaTraTienGiuNguyen() {
        Order order = taoDon(OrderStatus.SHIPPING, PaymentMethod.VNPAY, PaymentStatus.PAID);
        orderStatusService.change(order, OrderActor.SHIPPER, OrderStatus.DELIVERED, null);
        assertEquals(PaymentStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("Giao that bai: shipper chuyen sang tra hang - hoan tien, don da tra tien thi hoan tien")
    void giaoThatBaiThiHoanTien() {
        Order order = taoDon(OrderStatus.SHIPPING, PaymentMethod.MOMO, PaymentStatus.PAID);

        orderStatusService.change(order, OrderActor.SHIPPER, OrderStatus.RETURNED_REFUNDED, "Khach khong nghe may");

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.RETURNED_REFUNDED, sau.getStatus());
        assertEquals(PaymentStatus.REFUNDED, sau.getPaymentStatus());
    }

    @Test
    @DisplayName("Shipper khong duoc xac nhan don hay huy don")
    void shipperKhongDuocXacNhanHayHuyDon() {
        Order order = taoDon(OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);

        assertThrows(IllegalStateException.class,
                () -> orderStatusService.change(order, OrderActor.SHIPPER, OrderStatus.CONFIRMED, null));
        assertThrows(IllegalStateException.class,
                () -> orderStatusService.change(order, OrderActor.SHIPPER, OrderStatus.CANCELLED, null));
        assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Buoc chuyen hop le theo vai nhung don da ket thuc thi van bi chan")
    void donKetThucThiKhongDoiDuoc() {
        Order order = taoDon(OrderStatus.CANCELLED, PaymentMethod.COD, PaymentStatus.PENDING);
        assertThrows(IllegalStateException.class,
                () -> orderStatusService.change(order, OrderActor.ADMIN, OrderStatus.CONFIRMED, null));
    }

    @Test
    @DisplayName("Thieu trang thai moi thi bao loi, khong ghi gi")
    void thieuTrangThaiMoi() {
        Order order = taoDon(OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        assertThrows(IllegalStateException.class,
                () -> orderStatusService.change(order, OrderActor.VENDOR, null, null));
        assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Ghi chu dai qua 500 ky tu thi bi cat bot, khong lam vo CSDL")
    void ghiChuQuaDaiBiCatBot() {
        Order order = taoDon(OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        String ghiChuDai = "x".repeat(600);

        Order sau = orderStatusService.change(order, OrderActor.VENDOR, OrderStatus.CONFIRMED, ghiChuDai);

        assertNotNull(sau.getNote());
        assertEquals(500, sau.getNote().length(), "Phai cat bot cho vua cot note 500 ky tu");
    }

    @Test
    @DisplayName("Khong ghi chu thi ghi chu cu giu nguyen")
    void khongGhiChuThiGiuNguyen() {
        Order order = taoDon(OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        order.setNote("Khach xin it da");
        orderRepository.save(order);

        orderStatusService.change(order, OrderActor.VENDOR, OrderStatus.CONFIRMED, null);

        assertEquals("Khach xin it da", orderRepository.findById(order.getId()).orElseThrow().getNote());
    }
}