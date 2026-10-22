package com.utetra.module.vendor;

import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Shipment;
import com.utetra.entity.Shop;
import com.utetra.module.orderstatus.OrderActor;
import com.utetra.module.orderstatus.OrderStatusService;
import com.utetra.module.orderstatus.OrderStatusTransitions;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Quan ly don hang cua shop theo 7 trang thai (tuan 3).
 *
 * Lop nay lo phan "don nay co thuoc shop cua toi khong" (chan IDOR), con phan
 * "buoc chuyen nay co hop le khong va co tac dong gi" do OrderStatusService
 * (goi chung voi man hinh Shipper o tuan 5) dam nhiem.
 */
@Service
public class VendorOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStatusService orderStatusService;
    private final VendorShopService vendorShopService;

    public VendorOrderService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              ShipmentRepository shipmentRepository,
                              OrderStatusService orderStatusService,
                              VendorShopService vendorShopService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderStatusService = orderStatusService;
        this.vendorShopService = vendorShopService;
    }

    /** Danh sach don cua shop, loc theo trang thai (null = tat ca). */
    @Transactional(readOnly = true)
    public Page<Order> listMyOrders(String username, OrderStatus status, int page, int size) {
        Shop shop = vendorShopService.requireMyShop(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        if (status == null) {
            return orderRepository.findByShopOrderByIdDesc(shop, pageable);
        }
        return orderRepository.findByShopAndStatusOrderByIdDesc(shop, status, pageable);
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countByStatus(String username) {
        Shop shop = vendorShopService.requireMyShop(username);
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, orderRepository.countByShopAndStatus(shop, status));
        }
        return counts;
    }

    /** Diem kiem tra chu so huu don hang, chan IDOR cho moi thao tac cua Vendor. */
    @Transactional(readOnly = true)
    public Order requireOwnedOrder(String username, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang #" + orderId));
        if (order.getShop() == null
                || order.getShop().getOwner() == null
                || !order.getShop().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Don hang #" + orderId + " khong thuoc shop cua tai khoan " + username);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> itemsOf(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    @Transactional(readOnly = true)
    public Shipment shipmentOf(Order order) {
        return shipmentRepository.findByOrder(order).orElse(null);
    }

    /** Cac trang thai Vendor duoc phep chuyen toi tu trang thai hien tai cua don. */
    public List<OrderStatus> nextStatesForVendor(OrderStatus current) {
        return OrderStatusTransitions.nextStates(OrderActor.VENDOR, current).stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
    }

    /**
     * Doi trang thai don hang cua shop minh.
     *
     * @throws IllegalStateException neu buoc chuyen khong nam trong luong hop le
     */
    @Transactional
    public Order changeStatus(String username, Long orderId, OrderStatus newStatus, String note) {
        Order order = requireOwnedOrder(username, orderId);
        return orderStatusService.change(order, OrderActor.VENDOR, newStatus, note);
    }
}