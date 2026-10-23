package com.utetra.module.realtime;

import com.utetra.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Ban that cua OrderStatusPublisher: day su kien qua STOMP (tuan 4).
 *
 * Kenh su dung (khop voi WebSocketConfig cua Quan, khong phai sua file dung chung):
 *   - Khach dat don:  /user/{username}/queue/orders
 *   - Shop (Vendor):  /topic/shop/{shopId}/orders     <- moi nguoi trong shop nghe duoc
 *   - Shipper:        /user/{username}/queue/deliveries
 *
 * Duong dan /user/... la kenh rieng cua tung tai khoan: Spring se tu them tien to
 * /user/ va chi nguoi dang nhap dung username do moi nhan duoc.
 */
@Component
public class StompOrderStatusPublisher implements OrderStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(StompOrderStatusPublisher.class);

    private static final String CUSTOMER_QUEUE = "/queue/orders";
    private static final String SHIPPER_QUEUE = "/queue/deliveries";

    private final SimpMessagingTemplate messagingTemplate;

    public StompOrderStatusPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishOrderStatus(Order order, String message) {
        OrderStatusEvent event = OrderStatusEvent.of(order, message, false);

        if (order.getCustomer() != null) {
            sendToUser(order.getCustomer().getUsername(), CUSTOMER_QUEUE, event, "khach");
        }
        if (order.getShop() != null) {
            messagingTemplate.convertAndSend(shopTopic(order.getShop().getId()), event);
            if (order.getShop().getOwner() != null) {
                sendToUser(order.getShop().getOwner().getUsername(), CUSTOMER_QUEUE, event, "chu shop");
            }
        }
        if (order.getShipper() != null) {
            sendToUser(order.getShipper().getUsername(), SHIPPER_QUEUE, event, "shipper");
        }
    }

    @Override
    public void publishNewOrderToShop(Order order) {
        if (order.getShop() == null) {
            return;
        }
        OrderStatusEvent event = OrderStatusEvent.of(order, "Shop co don moi " + order.getCode(), true);
        messagingTemplate.convertAndSend(shopTopic(order.getShop().getId()), event);
        if (order.getShop().getOwner() != null) {
            sendToUser(order.getShop().getOwner().getUsername(), CUSTOMER_QUEUE, event, "chu shop");
        }
    }

    private String shopTopic(Long shopId) {
        return "/topic/shop/" + shopId + "/orders";
    }

    private void sendToUser(String username, String destination, OrderStatusEvent event, String vaiTro) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, event);
        } catch (RuntimeException ex) {
            // Nguoi dung khong mo trang (khong co phien WebSocket) khong duoc lam hong
            // giao dich doi trang thai don hang.
            log.debug("Khong day duoc thong bao toi {} {}: {}", vaiTro, username, ex.getMessage());
        }
    }
}