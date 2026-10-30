package com.utetra.module.vendor;

import com.utetra.entity.Carrier;
import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Product;
import com.utetra.entity.Shipment;
import com.utetra.entity.Shop;
import com.utetra.repository.CarrierRepository;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu don hang cua Vendor (tuan 3):
 * luong 7 trang thai, chan IDOR, va cac tac dong kem theo (COD, hoan tien).
 */
@SpringBootTest
@Transactional
class VendorOrderServiceTest {

    @Autowired VendorOrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CarrierRepository carrierRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    private Order taoDon(Shop shop, OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setCode("TEST-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        order.setSubtotal(BigDecimal.valueOf(50000));
        order.setShippingFee(BigDecimal.valueOf(15000));
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(BigDecimal.valueOf(65000));
        order.setStatus(status);
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    private Shipment taoShipment(Order order) {
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipper(userRepository.findByUsername("shipper").orElseThrow());
        Carrier carrier = carrierRepository.findAll().get(0);
        shipment.setCarrier(carrier);
        shipment.setFee(BigDecimal.valueOf(15000));
        shipment.setAssignedAt(LocalDateTime.now());
        return shipmentRepository.save(shipment);
    }

    @Test
    @DisplayName("Vendor xac nhan don moi, ly do duoc ghi them vao ghi chu")
    void xacNhanDonMoiVaLuuGhiChu() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);

        Order sau = orderService.changeStatus("vendor1", order.getId(), OrderStatus.CONFIRMED, "Da goi xac nhan khach");

        assertEquals(OrderStatus.CONFIRMED, sau.getStatus());
        assertNotNull(sau.getNote());
        assertTrue(sau.getNote().contains("Da goi xac nhan khach"));
    }

    @Test
    @DisplayName("Khong the nhay coc trang thai, du goi thang API")
    void khongNhayCocTrangThai() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.changeStatus("vendor1", order.getId(), OrderStatus.DELIVERED, null));
        assertTrue(ex.getMessage().contains("Khong the chuyen"));
        assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Doi sang dung trang thai hien tai thi bao loi ro rang")
    void doiSangChinhTrangThaiHienTai() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.CONFIRMED, PaymentMethod.COD, PaymentStatus.PENDING);
        assertThrows(IllegalStateException.class,
                () -> orderService.changeStatus("vendor1", order.getId(), OrderStatus.CONFIRMED, null));
    }

    @Test
    @DisplayName("Don da huy thi khong doi duoc nua")
    void donDaHuyThiDungHan() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        orderService.changeStatus("vendor1", order.getId(), OrderStatus.CANCELLED, "Khach doi y");

        assertThrows(IllegalStateException.class,
                () -> orderService.changeStatus("vendor1", order.getId(), OrderStatus.CONFIRMED, null));
    }

    @Test
    @DisplayName("IDOR: Vendor khac khong doc / doi trang thai duoc don cua shop minh")
    void vendorKhacKhongChamDuocDon() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);

        assertThrows(AccessDeniedException.class,
                () -> orderService.requireOwnedOrder("vendor2", order.getId()));
        assertThrows(AccessDeniedException.class,
                () -> orderService.changeStatus("vendor2", order.getId(), OrderStatus.CONFIRMED, null));

        assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Vendor khong duoc tu danh dau da giao khi don dang tren duong")
    void vendorKhongTuDanhDauDaGiao() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.SHIPPING, PaymentMethod.COD, PaymentStatus.PENDING);
        taoShipment(order);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.changeStatus("vendor1", order.getId(), OrderStatus.DELIVERED, null));
        assertTrue(ex.getMessage().contains("Khong the chuyen"));
        assertEquals(OrderStatus.SHIPPING, orderRepository.findById(order.getId()).orElseThrow().getStatus(),
                "Don phai dung nguyen o trang thai dang giao cho shipper xu ly");
    }

    @Test
    @DisplayName("Don da tra tien ma bi huy thi danh dau da hoan tien")
    void donDaTraTienBiHuyThiHoanTien() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.VNPAY, PaymentStatus.PAID);

        orderService.changeStatus("vendor1", order.getId(), OrderStatus.CANCELLED, "Het nguyen lieu");

        assertEquals(PaymentStatus.REFUNDED,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("Don chua tra tien ma bi huy thi khong doi trang thai thanh toan")
    void donChuaTraTienBiHuyThiGiuNguyen() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.CONFIRMED, PaymentMethod.COD, PaymentStatus.PENDING);

        orderService.changeStatus("vendor1", order.getId(), OrderStatus.CANCELLED, null);

        assertEquals(PaymentStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
        assertNull(orderRepository.findById(order.getId()).orElseThrow().getNote());
    }

    @Test
    @DisplayName("Don tra hang - hoan tien: chi di tiep khi don dang giao hoac da giao")
    void traHangChiTuDonDangGiaoHoacDaGiao() {
        Order dangGiao = taoDon(shopOf("vendor1"), OrderStatus.SHIPPING, PaymentMethod.COD, PaymentStatus.PAID);
        orderService.changeStatus("vendor1", dangGiao.getId(), OrderStatus.RETURNED_REFUNDED, "Khach tu choi nhan");
        assertEquals(OrderStatus.RETURNED_REFUNDED,
                orderRepository.findById(dangGiao.getId()).orElseThrow().getStatus());
        assertEquals(PaymentStatus.REFUNDED,
                orderRepository.findById(dangGiao.getId()).orElseThrow().getPaymentStatus());

        Order donMoi = taoDon(shopOf("vendor1"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        assertThrows(IllegalStateException.class, () -> orderService.changeStatus(
                "vendor1", donMoi.getId(), OrderStatus.RETURNED_REFUNDED, null));
    }

    @Test
    @DisplayName("Danh sach don chi tra ve don cua shop minh, loc theo trang thai")
    void danhSachDonCuaShopMinh() {
        Shop shop1 = shopOf("vendor1");
        taoDon(shop1, OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);
        taoDon(shop1, OrderStatus.DELIVERED, PaymentMethod.COD, PaymentStatus.PAID);
        taoDon(shopOf("vendor2"), OrderStatus.NEW, PaymentMethod.COD, PaymentStatus.PENDING);

        var tatCa = orderService.listMyOrders("vendor1", null, 0, 50);
        assertTrue(tatCa.getContent().stream()
                .allMatch(o -> o.getShop().getId().equals(shop1.getId())));

        var donMoi = orderService.listMyOrders("vendor1", OrderStatus.NEW, 0, 50);
        assertTrue(donMoi.getContent().stream().allMatch(o -> o.getStatus() == OrderStatus.NEW));

        assertTrue(orderService.countByStatus("vendor1").get(OrderStatus.DELIVERED) >= 1);
    }

    @Test
    @DisplayName("Chi tiet don: lay duoc mon trong don va thong tin giao hang")
    void chiTietDon() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.CONFIRMED, PaymentMethod.COD, PaymentStatus.PENDING);
        Product product = productRepository
                .findByShopOrderByIdDesc(shopOf("vendor1"), PageRequest.of(0, 1)).getContent().get(0);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setVariantName("Size M");
        item.setUnitPrice(BigDecimal.valueOf(40000));
        item.setQuantity(2);
        item.setLineTotal(BigDecimal.valueOf(80000));
        orderItemRepository.save(item);

        Shipment shipment = taoShipment(order);

        assertEquals(1, orderService.itemsOf(order).size());
        assertEquals(shipment.getId(), orderService.shipmentOf(order).getId());
        assertEquals(2, orderService.nextStatesForVendor(OrderStatus.CONFIRMED).size());
    }

    // ============================================== hoa don ban cua Vendor

    @Autowired com.utetra.module.invoice.InvoiceService invoiceService;

    @Test
    @DisplayName("Vendor xem duoc hoa don don cua shop minh khi don da thanh toan")
    void vendorXemDuocHoaDonDonDaThanhToan() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.DELIVERED,
                PaymentMethod.COD, PaymentStatus.PAID);
        taoShipment(order);

        Order cuaToi = orderService.requireOwnedOrder("vendor1", order.getId());
        invoiceService.requirePaid(cuaToi);
        var lichSu = invoiceService.timelineOf(cuaToi);

        assertFalse(lichSu.isEmpty());
        assertEquals("Khách đặt hàng", lichSu.get(0).ten());
    }

    @Test
    @DisplayName("Don chua thanh toan thi chua xuat duoc hoa don")
    void donChuaThanhToanThiKhongCoHoaDon() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW,
                PaymentMethod.COD, PaymentStatus.PENDING);

        assertThrows(IllegalStateException.class,
                () -> invoiceService.requirePaid(order));
    }

    @Test
    @DisplayName("Vendor khac khong mo duoc hoa don don khong phai cua minh")
    void vendorKhacKhongMoDuocHoaDon() {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.DELIVERED,
                PaymentMethod.COD, PaymentStatus.PAID);

        assertThrows(Exception.class,
                () -> orderService.requireOwnedOrder("vendor2", order.getId()));
    }
}
