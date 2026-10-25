package com.utetra.module.realtime;

import com.utetra.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Du lieu day qua WebSocket moi khi don hang doi trang thai hoac co don moi (tuan 4).
 *
 * Dung record de tin hieu ro rang: day la du lieu bat bien, chi de hien thi.
 * Khong day ca entity Order vi entity co quan he LAZY, Jackson se loi khi
 * tuan tu hoa ngoai phien lam viec.
 */
public record OrderStatusEvent(
        Long orderId,
        String code,
        String status,
        String statusLabel,
        Long shopId,
        String shopName,
        String customerName,
        BigDecimal total,
        LocalDateTime occurredAt,
        String message,
        boolean newOrder) {

    public static OrderStatusEvent of(Order order, String message, boolean newOrder) {
        return new OrderStatusEvent(
                order.getId(),
                order.getCode(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getStatus() == null ? null : order.getStatus().getLabel(),
                order.getShop() == null ? null : order.getShop().getId(),
                order.getShop() == null ? null : order.getShop().getName(),
                order.getReceiverName(),
                order.getTotal(),
                LocalDateTime.now(),
                message,
                newOrder);
    }
}