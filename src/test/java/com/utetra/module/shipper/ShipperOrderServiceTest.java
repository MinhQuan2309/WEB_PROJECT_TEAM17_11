package com.utetra.module.shipper;

import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shipment;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu shipper (tuan 5): nhan don, cap nhat trang thai, ly do giao that bai,
 * thong ke COD, va chan IDOR.
 */
@SpringBootTest
@Transactional
class ShipperOrderServiceTest {

    @Autowired ShipperOrderService shipperOrderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager entityManager;

    private User shipper(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    private void datLaiNgayTao(Order order, java.time.LocalDateTime thoiDiem) {
        entityManager.createNativeQuery("update orders set created_at = :thoiDiem where id = :id")
                .setParameter("thoiDiem", thoiDiem)
                .setParameter("id", order.getId())
                .executeUpdate();
        order.setCreatedAt(thoiDiem);
    }

    private Order taoDon(OrderStatus status, User shipperDuocPhanCong, PaymentMethod method,
                         PaymentStatus paymentStatus, String total, java.time.LocalDateTime thoiDiem) {
        Order order = new Order();
        order.setCode("SHP-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shopOf("vendor1"));
        order.setShipper(shipperDuocPhanCong);
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        order.setSubtotal(new BigDecimal(total));
        order.setShippingFee(BigDecimal.valueOf(15000));
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal(total));
        order.setStatus(status);
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        Order daLuu = orderRepository.save(order);
        datLaiNgayTao(daLuu, thoiDiem);
        return daLuu;
    }

    @Test
    @DisplayName("Nhan don: chi nhan don da xac nhan, tao ban ghi giao hang va ma van don")
    void nhanDonTaoBanGhiGiaoHang() {
        Order order = taoDon(OrderStatus.CONFIRMED, null, PaymentMethod.COD, PaymentStatus.PENDING,
                "80000", java.time.LocalDateTime.now());

        Order sau = shipperOrderService.claimOrder("shipper", order.getId());

        assertEquals("shipper", sau.getShipper().getUsername());
        assertEquals(OrderStatus.CONFIRMED, sau.getStatus(), "Nhan don khong doi trang thai don");

        Shipment shipment = shipmentRepository.findByOrder(sau).orElseThrow();
        assertEquals("shipper", shipment.getShipper().getUsername());
        assertNotNull(shipment.getAssignedAt());
        assertNotNull(shipment.getTrackingCode());
        assertEquals(0, new BigDecimal("15000").compareTo(shipment.getFee()));
    }

    @Test
    @DisplayName("Khong nhan duoc don chua duoc shop xac nhan")
    void khongNhanDonChuaXacNhan() {
        Order order = taoDon(OrderStatus.NEW, null, PaymentMethod.COD, PaymentStatus.PENDING,
                "80000", java.time.LocalDateTime.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> shipperOrderService.claimOrder("shipper", order.getId()));
        assertTrue(ex.getMessage().contains("xac nhan"));
    }

    @Test
    @DisplayName("Khong nhan lai don da co shipper")
    void khongNhanDonDaCoNguoi() {
        User shipperKhac = shipper("shipper");
        Order order = taoDon(OrderStatus.CONFIRMED, shipperKhac, PaymentMethod.COD, PaymentStatus.PENDING,
                "80000", java.time.LocalDateTime.now());

        assertThrows(IllegalStateException.class,
                () -> shipperOrderService.claimOrder("shipper", order.getId()));
    }

    @Test
    @DisplayName("Don cho nhan: chi hien don da xac nhan va chua co shipper")
    void danhSachDonChoNhan() {
        Order choNhan = taoDon(OrderStatus.CONFIRMED, null, PaymentMethod.COD, PaymentStatus.PENDING,
                "80000", java.time.LocalDateTime.now());
        Order daCoNguoi = taoDon(OrderStatus.CONFIRMED, shipper("shipper"), PaymentMethod.COD,
                PaymentStatus.PENDING, "80000", java.time.LocalDateTime.now());
        Order donMoi = taoDon(OrderStatus.NEW, null, PaymentMethod.COD, PaymentStatus.PENDING,
                "80000", java.time.LocalDateTime.now());

        var danhSach = shipperOrderService.listAvailableOrders(0, 50).getContent();

        assertTrue(danhSach.stream().anyMatch(o -> o.getId().equals(choNhan.getId())));
        assertTrue(danhSach.stream().noneMatch(o -> o.getId().equals(daCoNguoi.getId())));
        assertTrue(danhSach.stream().noneMatch(o -> o.getId().equals(donMoi.getId())));
    }

    @Test
    @DisplayName("Luong giao hang: nhan don -> da lay hang -> dang giao -> giao xong, COD chuyen sang da tra tien")
    void luongGiaoHangDayDu() {
        Order order = taoDon(OrderStatus.CONFIRMED, null, PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());
        shipperOrderService.claimOrder("shipper", order.getId());

        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.PICKED_UP, null, null);
        assertEquals(OrderStatus.PICKED_UP, orderRepository.findById(order.getId()).orElseThrow().getStatus());

        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.SHIPPING, null, null);
        Order dangGiao = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.SHIPPING, dangGiao.getStatus());
        assertNotNull(shipmentRepository.findByOrder(dangGiao).orElseThrow().getPickedUpAt());

        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.DELIVERED, null, null);
        Order xong = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.DELIVERED, xong.getStatus());
        assertEquals(PaymentStatus.PAID, xong.getPaymentStatus(), "COD giao xong la da thu tien");
        assertNotNull(shipmentRepository.findByOrder(xong).orElseThrow().getDeliveredAt());
    }

    @Test
    @DisplayName("Giao that bai: bat buoc co ly do, luu vao Shipment va ghi chu don hang")
    void giaoThatBaiPhaiCoLyDo() {
        Order order = taoDon(OrderStatus.CONFIRMED, null, PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());
        shipperOrderService.claimOrder("shipper", order.getId());
        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.PICKED_UP, null, null);
        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.SHIPPING, null, null);

        assertThrows(IllegalStateException.class, () -> shipperOrderService
                .updateStatus("shipper", order.getId(), OrderStatus.RETURNED_REFUNDED, null, "   "));
        assertEquals(OrderStatus.SHIPPING, orderRepository.findById(order.getId()).orElseThrow().getStatus());

        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.RETURNED_REFUNDED,
                null, "Khach khong nghe may 3 lan");

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.RETURNED_REFUNDED, sau.getStatus());
        assertTrue(sau.getNote().contains("Khach khong nghe may 3 lan"));
        assertEquals("Khach khong nghe may 3 lan",
                shipmentRepository.findByOrder(sau).orElseThrow().getFailedReason());
    }

    @Test
    @DisplayName("Shipper khong duoc tu xac nhan hoac huy don")
    void shipperKhongTuXacNhanHayHuyDon() {
        Order order = taoDon(OrderStatus.CONFIRMED, shipper("shipper"), PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> shipperOrderService
                .updateStatus("shipper", order.getId(), OrderStatus.CANCELLED, null, null));
        assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("IDOR: shipper khac khong xem / cap nhat duoc don cua nguoi khac")
    void shipperKhacKhongChamDuocDon() {
        Order order = taoDon(OrderStatus.SHIPPING, shipper("shipper"), PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());

        assertThrows(AccessDeniedException.class,
                () -> shipperOrderService.requireAssignedOrder("vendor1", order.getId()));
        assertThrows(AccessDeniedException.class, () -> shipperOrderService
                .updateStatus("vendor1", order.getId(), OrderStatus.DELIVERED, null, null));

        assertEquals(OrderStatus.SHIPPING, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Danh sach don cua toi: chi tra ve don duoc phan cong cho minh")
    void danhSachDonCuaToi() {
        User shipper = shipper("shipper");
        Order cuaToi = taoDon(OrderStatus.SHIPPING, shipper, PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());
        Order cuaNguoiKhac = taoDon(OrderStatus.SHIPPING, shipper("vendor1"), PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());

        var danhSach = shipperOrderService.listMyDeliveries("shipper", null, 0, 50).getContent();

        assertTrue(danhSach.stream().anyMatch(o -> o.getId().equals(cuaToi.getId())));
        assertTrue(danhSach.stream().noneMatch(o -> o.getId().equals(cuaNguoiKhac.getId())));
        assertTrue(danhSach.stream().allMatch(o -> o.getShipper().getUsername().equals("shipper")));

        var dangGiao = shipperOrderService.listMyDeliveries("shipper", OrderStatus.SHIPPING, 0, 50);
        assertTrue(dangGiao.getContent().stream().allMatch(o -> o.getStatus() == OrderStatus.SHIPPING));
    }

    @Test
    @DisplayName("Thu ho COD: don da tra truoc thi khong thu, don COD chua tra thi thu du tong tien")
    void tinhTienThuHo() {
        Order cod = taoDon(OrderStatus.SHIPPING, shipper("shipper"), PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());
        Order daTraTruoc = taoDon(OrderStatus.SHIPPING, shipper("shipper"), PaymentMethod.VNPAY,
                PaymentStatus.PAID, "90000", java.time.LocalDateTime.now());
        Order codDaThu = taoDon(OrderStatus.DELIVERED, shipper("shipper"), PaymentMethod.COD,
                PaymentStatus.PAID, "90000", java.time.LocalDateTime.now());

        assertEquals(0, new BigDecimal("90000").compareTo(shipperOrderService.codAmountOf(cod)));
        assertEquals(0, BigDecimal.ZERO.compareTo(shipperOrderService.codAmountOf(daTraTruoc)));
        assertEquals(0, BigDecimal.ZERO.compareTo(shipperOrderService.codAmountOf(codDaThu)));
    }

    @Test
    @DisplayName("Thong ke: dem dung don da giao / that bai / dang giao va tien COD")
    void thongKeGiaoHang() {
        User shipper = shipper("shipper");
        LocalDate homNay = LocalDate.now();

        Order daGiao1 = taoDon(OrderStatus.DELIVERED, shipper, PaymentMethod.COD,
                PaymentStatus.PAID, "100000", homNay.atTime(9, 0));
        Order daGiao2 = taoDon(OrderStatus.DELIVERED, shipper, PaymentMethod.VNPAY,
                PaymentStatus.PAID, "200000", homNay.atTime(10, 0));
        taoDon(OrderStatus.RETURNED_REFUNDED, shipper, PaymentMethod.COD,
                PaymentStatus.REFUNDED, "300000", homNay.atTime(11, 0));
        taoDon(OrderStatus.SHIPPING, shipper, PaymentMethod.COD,
                PaymentStatus.PENDING, "400000", homNay.atTime(12, 0));
        // Don cua shipper khac thi khong duoc tinh.
        taoDon(OrderStatus.DELIVERED, shipper("vendor1"), PaymentMethod.COD,
                PaymentStatus.PAID, "9000000", homNay.atTime(13, 0));

        var thongKe = shipperOrderService.stats("shipper", homNay, homNay,
                ShipperOrderService.GroupBy.DAY);

        assertEquals(2, thongKe.deliveredCount());
        assertEquals(1, thongKe.failedCount());
        assertEquals(1, thongKe.activeCount());
        assertEquals(0, new BigDecimal("100000").compareTo(thongKe.codCollected()),
                "Chi don COD da giao moi tinh vao tien thu ho");
        assertEquals(0, new BigDecimal("30000").compareTo(thongKe.shippingFeeEarned()),
                "Phi giao hang cua hai don da giao");
        assertEquals(1, thongKe.series().size());
        assertEquals(2, thongKe.series().get(0).delivered());
        assertEquals(0, new BigDecimal("100000").compareTo(thongKe.series().get(0).cod()));
        assertNotNull(daGiao1.getId());
        assertNotNull(daGiao2.getId());
    }

    @Test
    @DisplayName("Thong ke theo thang va khoang thoi gian nguoc")
    void thongKeTheoThangVaKhoangNguoc() {
        User shipper = shipper("shipper");
        LocalDate homNay = LocalDate.now();
        taoDon(OrderStatus.DELIVERED, shipper, PaymentMethod.COD, PaymentStatus.PAID,
                "50000", homNay.withDayOfMonth(1).atTime(8, 0));

        var theoThang = shipperOrderService.stats("shipper", homNay.withDayOfMonth(1), homNay,
                ShipperOrderService.GroupBy.MONTH);
        assertEquals(1, theoThang.series().size());
        assertEquals(1, theoThang.deliveredCount());

        var nguoc = shipperOrderService.stats("shipper", homNay, homNay.minusDays(3), null);
        assertEquals(homNay.minusDays(3), nguoc.from());
        assertEquals(homNay, nguoc.to());
    }

    @Test
    @DisplayName("Chi tiet don giao: lay duoc mon va ban ghi giao hang")
    void chiTietDonGiao() {
        Order order = taoDon(OrderStatus.CONFIRMED, null, PaymentMethod.COD,
                PaymentStatus.PENDING, "90000", java.time.LocalDateTime.now());
        shipperOrderService.claimOrder("shipper", order.getId());
        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.PICKED_UP, null, null);
        shipperOrderService.updateStatus("shipper", order.getId(), OrderStatus.SHIPPING, null, null);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductName("Tra sua");
        item.setUnitPrice(BigDecimal.valueOf(30000));
        item.setQuantity(3);
        item.setLineTotal(BigDecimal.valueOf(90000));
        orderItemRepository.save(item);

        assertEquals(1, shipperOrderService.itemsOf(order).size());
        assertNotNull(shipperOrderService.shipmentOf(order));
        assertEquals(2, shipperOrderService.nextStatesForShipper(OrderStatus.SHIPPING).size());
        assertTrue(shipperOrderService.nextStatesForShipper(OrderStatus.DELIVERED).isEmpty());
    }
}