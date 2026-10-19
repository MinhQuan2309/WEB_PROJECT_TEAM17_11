package com.utetra.module.realtime;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Kiem tra viec day thong bao real-time (tuan 4): dung kenh nao, gui cho ai.
 * Dung Mockito gia lap SimpMessagingTemplate nen khong can WebSocket that.
 */
class StompOrderStatusPublisherTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final StompOrderStatusPublisher publisher = new StompOrderStatusPublisher(template);

    private User nguoiDung(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private Order donHang() {
        Shop shop = new Shop();
        shop.setId(5L);
        shop.setName("Quan An Vat");
        shop.setOwner(nguoiDung("vendor1"));

        Order order = new Order();
        order.setId(11L);
        order.setCode("DH0011");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setShop(shop);
        order.setCustomer(nguoiDung("khach"));
        order.setShipper(nguoiDung("shipper"));
        order.setReceiverName("Khach Le");
        order.setTotal(BigDecimal.valueOf(99000));
        return order;
    }

    @Test
    @DisplayName("Doi trang thai: day cho ca khach, shop, chu shop va shipper")
    void dayTrangThaiChoCacBenLienQuan() {
        publisher.publishOrderStatus(donHang(), "Don DH0011: Da xac nhan");

        verify(template).convertAndSend(eq("/topic/shop/5/orders"), any(OrderStatusEvent.class));
        verify(template).convertAndSendToUser(eq("khach"), eq("/queue/orders"), any(OrderStatusEvent.class));
        verify(template).convertAndSendToUser(eq("vendor1"), eq("/queue/orders"), any(OrderStatusEvent.class));
        verify(template).convertAndSendToUser(eq("shipper"), eq("/queue/deliveries"), any(OrderStatusEvent.class));
    }

    @Test
    @DisplayName("Don chua co shipper thi khong day cho ai khac")
    void donChuaCoShipper() {
        Order order = donHang();
        order.setShipper(null);

        publisher.publishOrderStatus(order, "Don DH0011: Da xac nhan");

        verify(template, never()).convertAndSendToUser(eq("shipper"), eq("/queue/deliveries"), any());
        verify(template).convertAndSend(eq("/topic/shop/5/orders"), any(OrderStatusEvent.class));
    }

    @Test
    @DisplayName("Don moi: chi bao cho shop, khong bao cho khach")
    void baoDonMoiChoShop() {
        publisher.publishNewOrderToShop(donHang());

        verify(template).convertAndSend(eq("/topic/shop/5/orders"), any(OrderStatusEvent.class));
        verify(template).convertAndSendToUser(eq("vendor1"), eq("/queue/orders"), any(OrderStatusEvent.class));
        verify(template, never()).convertAndSendToUser(eq("khach"), eq("/queue/orders"), any());
    }

    @Test
    @DisplayName("Khong co shop thi bo qua, khong nem loi")
    void khongCoShopThiBoQua() {
        Order order = donHang();
        order.setShop(null);

        assertDoesNotThrow(() -> publisher.publishNewOrderToShop(order));
        verify(template, never()).convertAndSend(any(String.class), any(OrderStatusEvent.class));
    }

    @Test
    @DisplayName("Gui loi (khong co phien WebSocket) khong duoc lam hong giao dich doi trang thai")
    void guiLoiKhongLamHongGiaoDich() {
        doThrow(new IllegalStateException("Khong co phien")).when(template)
                .convertAndSendToUser(any(String.class), any(String.class), any(OrderStatusEvent.class));

        assertDoesNotThrow(() -> publisher.publishOrderStatus(donHang(), "Don DH0011: Da xac nhan"));
        verify(template).convertAndSend(eq("/topic/shop/5/orders"), any(OrderStatusEvent.class));
    }
}