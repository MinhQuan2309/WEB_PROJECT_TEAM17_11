package com.utetra.module.shipper;

import com.utetra.entity.Carrier;
import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.Shipment;
import com.utetra.entity.User;
import com.utetra.module.orderstatus.OrderActor;
import com.utetra.module.orderstatus.OrderStatusService;
import com.utetra.module.orderstatus.OrderStatusTransitions;
import com.utetra.module.realtime.OrderStatusPublisher;
import com.utetra.repository.CarrierRepository;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
import com.utetra.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiep vu cua shipper (tuan 5).
 *
 * Ba nhom viec:
 *   1. Nhan don: don da duoc shop xac nhan va chua co ai nhan thi shipper nhan duoc.
 *      Nhan don se tao ban ghi Shipment (ma van don, phi giao, moc nhan don).
 *   2. Cap nhat trang thai: CONFIRMED -> PICKED_UP -> SHIPPING -> DELIVERED, hoac
 *      SHIPPING -> RETURNED_REFUNDED khi giao that bai (bat buoc phai co ly do).
 *      Moi buoc chuyen deu qua OrderStatusService nen dung chung luat voi Vendor.
 *   3. Thong ke: so don da giao / that bai / dang giao, tien COD da thu, phi giao hang,
 *      va bang theo ngay hoac theo thang.
 *
 * Moi thao tac doc/ghi deu qua requireAssignedOrder: shipper khong cham duoc don cua
 * nguoi khac (chan IDOR).
 */
@Service
public class ShipperOrderService {

    /** Kieu gop nhom cho bang thong ke. */
    public enum GroupBy {
        DAY, MONTH
    }

    public record ShipperPoint(String label, long delivered, BigDecimal cod) {
    }

    public record ShipperStats(LocalDate from,
                               LocalDate to,
                               GroupBy groupBy,
                               long deliveredCount,
                               long failedCount,
                               long activeCount,
                               BigDecimal codCollected,
                               BigDecimal shippingFeeEarned,
                               List<ShipperPoint> series) {
    }

    private static final int MAX_FAILED_REASON = 500;
    private static final DateTimeFormatter LABEL_DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LABEL_MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;
    private final OrderStatusService orderStatusService;
    private final OrderStatusPublisher orderStatusPublisher;

    public ShipperOrderService(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               ShipmentRepository shipmentRepository,
                               CarrierRepository carrierRepository,
                               UserRepository userRepository,
                               OrderStatusService orderStatusService,
                               OrderStatusPublisher orderStatusPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.carrierRepository = carrierRepository;
        this.userRepository = userRepository;
        this.orderStatusService = orderStatusService;
        this.orderStatusPublisher = orderStatusPublisher;
    }

    // ===== Doc du lieu =====

    @Transactional(readOnly = true)
    public Page<Order> listMyDeliveries(String username, OrderStatus status, int page, int size) {
        User shipper = requireShipper(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        if (status == null) {
            return orderRepository.findByShipperOrderByIdDesc(shipper, pageable);
        }
        return orderRepository.findByShipperAndStatusOrderByIdDesc(shipper, status, pageable);
    }

    /** Don da xac nhan nhung chua co shipper nao nhan. */
    @Transactional(readOnly = true)
    public Page<Order> listAvailableOrders(int page, int size) {
        return orderRepository.findByStatusAndShipperIsNullOrderByIdAsc(
                OrderStatus.CONFIRMED, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countByStatus(String username) {
        User shipper = requireShipper(username);
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, orderRepository.countByShipperAndStatus(shipper, status));
        }
        return counts;
    }

    /** Diem kiem tra chu so huu don giao: chan IDOR cho moi thao tac cua shipper. */
    @Transactional(readOnly = true)
    public Order requireAssignedOrder(String username, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang #" + orderId));
        if (order.getShipper() == null
                || !order.getShipper().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Don hang #" + orderId + " khong duoc phan cong cho tai khoan " + username);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Shipment shipmentOf(Order order) {
        return shipmentRepository.findByOrder(order).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> itemsOf(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    /** Cac buoc di tiep ma shipper duoc phep lam voi don nay. */
    public List<OrderStatus> nextStatesForShipper(OrderStatus current) {
        return OrderStatusTransitions.nextStates(OrderActor.SHIPPER, current).stream()
                .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
                .toList();
    }

    /** Tien phai thu khi giao: chi don COD moi thu tien mat. */
    public BigDecimal codAmountOf(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            return BigDecimal.ZERO;
        }
        if (order.getPaymentStatus() == com.utetra.entity.PaymentStatus.PAID) {
            return BigDecimal.ZERO;
        }
        return order.getTotal() == null ? BigDecimal.ZERO : order.getTotal();
    }

    // ===== Thao tac =====

    /**
     * Nhan don: chi nhan don da duoc shop xac nhan va chua co shipper.
     * Tao luon ban ghi Shipment neu chua co.
     */
    @Transactional
    public Order claimOrder(String username, Long orderId) {
        User shipper = requireShipper(username);
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang #" + orderId));

        if (order.getShipper() != null) {
            if (order.getShipper().getUsername().equals(username)) {
                throw new IllegalStateException("Ban da nhan don nay roi");
            }
            throw new IllegalStateException("Don nay da co shipper khac nhan");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Chi nhan duoc don da duoc shop xac nhan ("
                    + OrderStatus.CONFIRMED.getLabel() + "), don nay dang o \""
                    + order.getStatus().getLabel() + "\"");
        }

        order.setShipper(shipper);
        Order daLuu = orderRepository.save(order);

        Shipment shipment = shipmentRepository.findByOrder(daLuu).orElseGet(Shipment::new);
        shipment.setOrder(daLuu);
        shipment.setShipper(shipper);
        if (shipment.getCarrier() == null && daLuu.getCarrier() != null) {
            shipment.setCarrier(daLuu.getCarrier());
        }
        if (shipment.getCarrier() == null) {
            carrierRepository.findAll().stream().findFirst().ifPresent(shipment::setCarrier);
        }
        if (shipment.getTrackingCode() == null || shipment.getTrackingCode().isBlank()) {
            shipment.setTrackingCode("TRK" + daLuu.getCode());
        }
        if (shipment.getFee() == null || shipment.getFee().compareTo(BigDecimal.ZERO) == 0) {
            shipment.setFee(daLuu.getShippingFee() == null ? BigDecimal.ZERO : daLuu.getShippingFee());
        }
        if (shipment.getAssignedAt() == null) {
            shipment.setAssignedAt(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        orderStatusPublisher.publishOrderStatus(daLuu,
                "Shipper " + shipper.getUsername() + " da nhan don " + daLuu.getCode());
        return daLuu;
    }

    /**
     * Cap nhat trang thai don dang giao.
     *
     * @param failedReason ly do bat buoc khi giao that bai (chuyen sang tra hang - hoan tien)
     */
    @Transactional
    public Order updateStatus(String username, Long orderId, OrderStatus newStatus,
                              String note, String failedReason) {
        Order order = requireAssignedOrder(username, orderId);

        if (newStatus == OrderStatus.RETURNED_REFUNDED) {
            if (failedReason == null || failedReason.isBlank()) {
                throw new IllegalStateException("Giao that bai phai ghi ro ly do");
            }
            if (failedReason.length() > MAX_FAILED_REASON) {
                throw new IllegalStateException("Ly do giao that bai toi da "
                        + MAX_FAILED_REASON + " ky tu");
            }
        }

        // Ly do giao that bai duoc luu o Shipment, dong thoi ghi vao ghi chu don hang
        // de chu shop nhin thay ngay trong trang quan ly don.
        String ghiChu = note;
        if (newStatus == OrderStatus.RETURNED_REFUNDED && (ghiChu == null || ghiChu.isBlank())) {
            ghiChu = "Giao that bai: " + failedReason.trim();
        }

        Order daLuu = orderStatusService.change(order, OrderActor.SHIPPER, newStatus, ghiChu);
        capNhatShipment(daLuu, newStatus, failedReason);
        return daLuu;
    }

    private void capNhatShipment(Order order, OrderStatus newStatus, String failedReason) {
        if (newStatus == OrderStatus.CONFIRMED) {
            return; // Chua lay hang thi chua can ban ghi giao hang.
        }

        Shipment shipment = shipmentRepository.findByOrder(order).orElse(null);
        boolean canLuu = false;

        if (shipment == null) {
            // Truong hop don duoc gan shipper ma chua co ban ghi giao hang: tao bu de
            // khong mat thong tin ma van don / moc thoi gian.
            shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setShipper(order.getShipper());
            if (order.getCarrier() != null) {
                shipment.setCarrier(order.getCarrier());
            } else {
                carrierRepository.findAll().stream().findFirst().ifPresent(shipment::setCarrier);
            }
            shipment.setTrackingCode("TRK" + order.getCode());
            shipment.setFee(order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee());
            shipment.setAssignedAt(LocalDateTime.now());
            canLuu = true;
        }

        if (newStatus == OrderStatus.SHIPPING && shipment.getPickedUpAt() == null) {
            shipment.setPickedUpAt(LocalDateTime.now());
            canLuu = true;
        }
        if (newStatus == OrderStatus.RETURNED_REFUNDED && failedReason != null
                && !failedReason.isBlank()) {
            shipment.setFailedReason(failedReason.trim());
            canLuu = true;
        }
        if (canLuu) {
            shipmentRepository.save(shipment);
        }
    }

    // ===== Thong ke =====

    @Transactional(readOnly = true)
    public ShipperStats stats(String username, LocalDate from, LocalDate to, GroupBy groupBy) {
        User shipper = requireShipper(username);

        LocalDate ngayKetThuc = to == null ? LocalDate.now() : to;
        LocalDate ngayBatDau = from == null ? ngayKetThuc.withDayOfMonth(1) : from;
        if (ngayBatDau.isAfter(ngayKetThuc)) {
            LocalDate tam = ngayBatDau;
            ngayBatDau = ngayKetThuc;
            ngayKetThuc = tam;
        }
        if (ngayBatDau.isBefore(ngayKetThuc.minusDays(365))) {
            ngayBatDau = ngayKetThuc.minusDays(365);
        }
        GroupBy kieuGop = groupBy == null ? GroupBy.DAY : groupBy;

        List<Order> donTrongKy = orderRepository.ordersOfShipperBetween(shipper,
                ngayBatDau.atStartOfDay(), ngayKetThuc.plusDays(1).atStartOfDay());

        long daGiao = 0;
        long thatBai = 0;
        long dangGiao = 0;
        BigDecimal tienCod = BigDecimal.ZERO;
        BigDecimal phiGiao = BigDecimal.ZERO;
        Map<String, Long> soDonTheoMoc = new LinkedHashMap<>();
        Map<String, BigDecimal> codTheoMoc = new LinkedHashMap<>();

        for (Order order : donTrongKy) {
            switch (order.getStatus()) {
                case DELIVERED -> {
                    daGiao++;
                    BigDecimal tong = order.getTotal() == null ? BigDecimal.ZERO : order.getTotal();
                    BigDecimal phi = order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();
                    phiGiao = phiGiao.add(phi);
                    if (order.getPaymentMethod() == PaymentMethod.COD) {
                        tienCod = tienCod.add(tong);
                    }
                    String moc = mocThoiGian(order.getCreatedAt(), kieuGop);
                    soDonTheoMoc.merge(moc, 1L, Long::sum);
                    codTheoMoc.merge(moc, order.getPaymentMethod() == PaymentMethod.COD
                            ? tong : BigDecimal.ZERO, BigDecimal::add);
                }
                case RETURNED_REFUNDED -> thatBai++;
                case CONFIRMED, PICKED_UP, SHIPPING -> dangGiao++;
                default -> {
                    // NEW / CANCELLED: khong tinh vao thong ke giao hang.
                }
            }
        }

        return new ShipperStats(ngayBatDau, ngayKetThuc, kieuGop, daGiao, thatBai, dangGiao,
                tienCod, phiGiao,
                duongThongKe(ngayBatDau, ngayKetThuc, kieuGop, soDonTheoMoc, codTheoMoc));
    }

    private List<ShipperPoint> duongThongKe(LocalDate tu, LocalDate den, GroupBy kieuGop,
                                            Map<String, Long> soDonTheoMoc,
                                            Map<String, BigDecimal> codTheoMoc) {
        List<ShipperPoint> diem = new ArrayList<>();
        if (kieuGop == GroupBy.MONTH) {
            YearMonth moc = YearMonth.from(tu);
            YearMonth cuoi = YearMonth.from(den);
            while (!moc.isAfter(cuoi)) {
                String nhan = moc.format(LABEL_MONTH);
                diem.add(new ShipperPoint(nhan, soDonTheoMoc.getOrDefault(nhan, 0L),
                        codTheoMoc.getOrDefault(nhan, BigDecimal.ZERO)));
                moc = moc.plusMonths(1);
            }
            return diem;
        }
        LocalDate ngay = tu;
        while (!ngay.isAfter(den)) {
            String nhan = ngay.format(LABEL_DAY);
            diem.add(new ShipperPoint(nhan, soDonTheoMoc.getOrDefault(nhan, 0L),
                    codTheoMoc.getOrDefault(nhan, BigDecimal.ZERO)));
            ngay = ngay.plusDays(1);
        }
        return diem;
    }

    private String mocThoiGian(LocalDateTime thoiDiem, GroupBy kieuGop) {
        LocalDate ngay = thoiDiem == null ? LocalDate.now() : thoiDiem.toLocalDate();
        return kieuGop == GroupBy.MONTH ? ngay.format(LABEL_MONTH) : ngay.format(LABEL_DAY);
    }

    private User requireShipper(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tai khoan " + username));
    }
}