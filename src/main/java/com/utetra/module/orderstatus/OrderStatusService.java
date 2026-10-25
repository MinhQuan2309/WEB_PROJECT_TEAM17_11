package com.utetra.module.orderstatus;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.module.realtime.OrderStatusPublisher;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Xu ly mot lan doi trang thai don hang: kiem tra luat + ghi cac tac dong kem theo
 * (Thien viet, dung chung cho man hinh Vendor tuan 3 va man hinh Shipper tuan 5).
 *
 * Vi sao tach rieng khoi VendorOrderService: khi shipper danh dau "da giao" thi cung
 * phai chay dung cac buoc nay (don COD -> da thanh toan, ghi moc giao hang). Neu de
 * trong VendorOrderService thi tuan 5 se phai chep lai, rat de lech nhau.
 *
 * Lop nay CHI kiem tra luat theo vai, khong kiem tra quyen so huu du lieu.
 * Quyen so huu (don nay co thuoc shop / co duoc phan cong cho shipper nay khong)
 * van nam o VendorOrderService / ShipperOrderService truoc khi goi vao day.
 */
@Service
public class OrderStatusService {

    /** Order.note gioi han 500 ky tu trong CSDL. */
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter NOTE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStatusPublisher orderStatusPublisher;

    public OrderStatusService(OrderRepository orderRepository,
                              ShipmentRepository shipmentRepository,
                              OrderStatusPublisher orderStatusPublisher) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderStatusPublisher = orderStatusPublisher;
    }

    /**
     * Doi trang thai don hang theo vai dang thao tac.
     *
     * @param actor vai dang doi (VENDOR / SHIPPER / CUSTOMER / ADMIN)
     * @throws IllegalStateException neu buoc chuyen khong hop le hoac don da ket thuc
     */
    @Transactional
    public Order change(Order order, OrderActor actor, OrderStatus newStatus, String note) {
        OrderStatus current = order.getStatus();

        if (newStatus == null) {
            throw new IllegalStateException("Thieu trang thai don hang moi");
        }
        if (current == newStatus) {
            throw new IllegalStateException("Don hang dang o trang thai \"" + current.getLabel() + "\" roi");
        }
        if (!OrderStatusTransitions.canMove(actor, current, newStatus)) {
            throw new IllegalStateException("Khong the chuyen don tu \"" + current.getLabel()
                    + "\" sang \"" + newStatus.getLabel() + "\"");
        }
        if (OrderStatusTransitions.isTerminal(current)) {
            throw new IllegalStateException("Don da o trang thai ket thuc, khong doi duoc nua");
        }

        order.setStatus(newStatus);
        if (note != null && !note.isBlank()) {
            order.setNote(appendNote(order.getNote(), note.trim()));
        }
        applySideEffects(order, newStatus);

        Order daLuu = orderRepository.save(order);
        // Day trang thai moi cho khach / shop / shipper dang mo trang (tuan 4).
        orderStatusPublisher.publishOrderStatus(daLuu,
                "Don " + daLuu.getCode() + ": " + newStatus.getLabel());
        return daLuu;
    }

    private void applySideEffects(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case DELIVERED -> {
                // Don COD: giao xong nghia la shipper da thu tien cua khach.
                if (order.getPaymentMethod() == PaymentMethod.COD
                        && order.getPaymentStatus() == PaymentStatus.PENDING) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                }
                shipmentRepository.findByOrder(order).ifPresent(shipment -> {
                    if (shipment.getDeliveredAt() == null) {
                        shipment.setDeliveredAt(LocalDateTime.now());
                        shipmentRepository.save(shipment);
                    }
                });
            }
            case CANCELLED, RETURNED_REFUNDED -> {
                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.REFUNDED);
                }
            }
            default -> {
                // CONFIRMED / PICKED_UP / SHIPPING: chua co tac dong them.
            }
        }
    }

    private String appendNote(String currentNote, String newLine) {
        String line = "[" + LocalDateTime.now().format(NOTE_TIME) + "] " + newLine;
        String combined = (currentNote == null || currentNote.isBlank()) ? line : currentNote + "\n" + line;
        if (combined.length() <= MAX_NOTE_LENGTH) {
            return combined;
        }
        // Giu lai phan moi nhat de khong vuot qua do dai cot trong CSDL.
        return combined.substring(combined.length() - MAX_NOTE_LENGTH);
    }
}