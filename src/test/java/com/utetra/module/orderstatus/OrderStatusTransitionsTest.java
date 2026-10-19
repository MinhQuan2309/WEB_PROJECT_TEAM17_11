package com.utetra.module.orderstatus;

import com.utetra.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra bo quy tac chuyen trang thai don hang (tuan 3).
 * Chay thuan unit test, khong can Spring context.
 */
class OrderStatusTransitionsTest {

    @Test
    @DisplayName("Luong chuan di duoc, nhay coc thi khong")
    void luongChuanDiDuoc() {
        assertTrue(OrderStatusTransitions.canMove(OrderStatus.NEW, OrderStatus.CONFIRMED));
        assertTrue(OrderStatusTransitions.canMove(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP));
        assertTrue(OrderStatusTransitions.canMove(OrderStatus.PICKED_UP, OrderStatus.SHIPPING));
        assertTrue(OrderStatusTransitions.canMove(OrderStatus.SHIPPING, OrderStatus.DELIVERED));

        assertFalse(OrderStatusTransitions.canMove(OrderStatus.NEW, OrderStatus.DELIVERED),
                "Khong duoc nhay tu don moi sang da giao");
        assertFalse(OrderStatusTransitions.canMove(OrderStatus.NEW, OrderStatus.PICKED_UP));
        assertFalse(OrderStatusTransitions.canMove(OrderStatus.CONFIRMED, OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Trang thai ket thuc thi khong di dau duoc nua")
    void trangThaiKetThucKhongDiTiep() {
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(OrderStatusTransitions.canMove(OrderStatus.CANCELLED, status));
            assertFalse(OrderStatusTransitions.canMove(OrderStatus.RETURNED_REFUNDED, status));
        }
        assertTrue(OrderStatusTransitions.isTerminal(OrderStatus.CANCELLED));
        assertTrue(OrderStatusTransitions.isTerminal(OrderStatus.RETURNED_REFUNDED));
        assertFalse(OrderStatusTransitions.isTerminal(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Phan quyen theo vai: shipper khong duoc huy don, vendor khong duoc tu giao hang")
    void phanQuyenTheoVai() {
        assertTrue(OrderStatusTransitions.canMove(OrderActor.SHIPPER, OrderStatus.SHIPPING, OrderStatus.DELIVERED));
        assertFalse(OrderStatusTransitions.canMove(OrderActor.SHIPPER, OrderStatus.NEW, OrderStatus.CONFIRMED),
                "Shipper khong duoc xac nhan don");

        assertTrue(OrderStatusTransitions.canMove(OrderActor.VENDOR, OrderStatus.NEW, OrderStatus.CONFIRMED));
        assertTrue(OrderStatusTransitions.canMove(OrderActor.VENDOR, OrderStatus.NEW, OrderStatus.CANCELLED));
        assertFalse(OrderStatusTransitions.canMove(OrderActor.VENDOR, OrderStatus.PICKED_UP, OrderStatus.DELIVERED),
                "Vendor khong duoc tu danh dau da giao khi don dang tren duong");

        assertTrue(OrderStatusTransitions.canMove(OrderActor.CUSTOMER, OrderStatus.NEW, OrderStatus.CANCELLED));
        assertFalse(OrderStatusTransitions.canMove(OrderActor.CUSTOMER, OrderStatus.NEW, OrderStatus.CONFIRMED));

        assertTrue(OrderStatusTransitions.canMove(OrderActor.ADMIN, OrderStatus.NEW, OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("Danh sach buoc di tiep cua tung vai")
    void danhSachBuocDiTiep() {
        assertEquals(2, OrderStatusTransitions.nextStates(OrderActor.VENDOR, OrderStatus.NEW).size());
        assertTrue(OrderStatusTransitions.nextStates(OrderActor.VENDOR, OrderStatus.NEW)
                .containsAll(java.util.Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)));
        assertTrue(OrderStatusTransitions.nextStates(OrderActor.VENDOR, OrderStatus.CANCELLED).isEmpty());

        assertEquals(java.util.Set.of(OrderStatus.DELIVERED, OrderStatus.RETURNED_REFUNDED),
                OrderStatusTransitions.nextStates(OrderActor.SHIPPER, OrderStatus.SHIPPING));
    }

    @Test
    @DisplayName("Dau vao null khong lam no ung dung")
    void dauVaoNull() {
        assertFalse(OrderStatusTransitions.canMove(null, OrderStatus.NEW));
        assertFalse(OrderStatusTransitions.canMove(OrderStatus.NEW, null));
        assertFalse(OrderStatusTransitions.canMove((OrderActor) null, OrderStatus.NEW, OrderStatus.CONFIRMED));
        assertTrue(OrderStatusTransitions.nextStates((OrderStatus) null).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> OrderStatusTransitions.nextStates(OrderStatus.NEW).add(OrderStatus.DELIVERED),
                "Tap buoc di tiep phai la bat bien, khong cho sua tu ben ngoai");
    }
}