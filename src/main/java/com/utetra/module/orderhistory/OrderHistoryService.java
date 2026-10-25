package com.utetra.module.orderhistory;

import com.utetra.entity.*;
import com.utetra.module.orderstatus.OrderActor;
import com.utetra.module.orderstatus.OrderStatusService;
import com.utetra.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Nghiep vu lich su mua hang cua khach theo trang thai, xem chi tiet don,
 * huy don va yeu cau tra hang - hoan tien (tuan 4).
 */
@Service
@Transactional
public class OrderHistoryService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final OrderStatusService orderStatusService;

    public OrderHistoryService(OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               ShipmentRepository shipmentRepository,
                               ProductRepository productRepository,
                               VoucherRepository voucherRepository,
                               UserRepository userRepository,
                               OrderStatusService orderStatusService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.productRepository = productRepository;
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.orderStatusService = orderStatusService;
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));
    }

    /** Danh sach don hang cua khach, loc theo trang thai (null = tat ca). */
    @Transactional(readOnly = true)
    public Page<Order> getCustomerOrders(User customer, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10);
        if (status == null) {
            return orderRepository.findByCustomerOrderByIdDesc(customer, pageable);
        }
        return orderRepository.findByCustomerAndStatusOrderByIdDesc(customer, status, pageable);
    }

    /** Dem so luong don cua khach theo tung tab trang thai. */
    @Transactional(readOnly = true)
    public Map<String, Long> countCustomerOrdersByStatus(User customer) {
        List<Order> allOrders = orderRepository
                .findByCustomerOrderByIdDesc(customer, PageRequest.of(0, 500))
                .getContent();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", (long) allOrders.size());
        for (OrderStatus st : OrderStatus.values()) {
            long c = allOrders.stream().filter(o -> o.getStatus() == st).count();
            counts.put(st.name(), c);
        }
        return counts;
    }

    /** Kiem tra quyen so huu don hang cua khach (chan IDOR). */
    @Transactional(readOnly = true)
    public Order getCustomerOrder(User customer, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn này."));
    }

    @Transactional(readOnly = true)
    public Optional<Shipment> getShipmentForOrder(Order order) {
        return shipmentRepository.findByOrder(order);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForOrder(Order order) {
        return paymentRepository.findByOrder(order);
    }

    /** Khach huy don khi don dang o trang thai NEW, dong thoi hoan lai luot ban va luot dung voucher. */
    public Order cancelOrder(User customer, Long orderId, String cancelReason) {
        Order order = getCustomerOrder(customer, orderId);
        String reason = (cancelReason != null && !cancelReason.isBlank())
                ? cancelReason.trim()
                : "Khách hàng hủy đơn";

        Order updated = orderStatusService.change(order, OrderActor.CUSTOMER, OrderStatus.CANCELLED, reason);

        // Hoan lai luot ban cua tung san pham trong don
        for (OrderItem item : updated.getItems()) {
            if (item.getProduct() != null) {
                Product p = item.getProduct();
                p.setSoldCount(Math.max(0, p.getSoldCount() - item.getQuantity()));
                productRepository.save(p);
            }
        }

        // Hoan lai luot su dung ma giam gia neu don co ap voucher
        if (updated.getVoucher() != null) {
            Voucher v = updated.getVoucher();
            v.setUsedCount(Math.max(0, v.getUsedCount() - 1));
            voucherRepository.save(v);
        }

        return updated;
    }

    /** Khach gui yeu cau tra hang - hoan tien khi don da o trang thai DELIVERED. */
    public Order requestReturnRefund(User customer, Long orderId, String returnReason) {
        Order order = getCustomerOrder(customer, orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Chỉ có thể yêu cầu trả hàng – hoàn tiền khi đơn ở trạng thái Đã giao.");
        }
        if (returnReason == null || returnReason.trim().length() < 10) {
            throw new IllegalArgumentException("Vui lòng nhập lý do trả hàng / hoàn tiền tối thiểu 10 ký tự.");
        }

        return orderStatusService.change(order, OrderActor.ADMIN, OrderStatus.RETURNED_REFUNDED, returnReason.trim());
    }
}
