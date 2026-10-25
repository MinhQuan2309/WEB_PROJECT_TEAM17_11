package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Payment;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import com.utetra.module.realtime.OrderStatusPublisher;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Nghiep vu thanh toan (tuan 4): bat dau thanh toan + xu ly callback/IPN.
 *
 * Lop nay KHONG co if/else theo tung cong thanh toan: moi viec rieng cua VNPAY/MOMO
 * da nam trong Strategy tuong ung (dung yeu cau "Strategy pattern" cua de tai).
 *
 * Ba chot an toan duoc kiem tra o day:
 *   1. Chi chu don hang moi duoc bat dau thanh toan cho don do (chan IDOR).
 *   2. Callback/IPN phai co chu ky hop le moi doi trang thai (chan gia mao).
 *   3. So tien cong bao ve phai dung bang tong tien cua don (chan sua so tien).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_GATEWAY_RESPONSE = 1000;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentStrategyRegistry strategyRegistry;
    private final OrderStatusPublisher orderStatusPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          PaymentStrategyRegistry strategyRegistry,
                          OrderStatusPublisher orderStatusPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.strategyRegistry = strategyRegistry;
        this.orderStatusPublisher = orderStatusPublisher;
    }

    /** Diem kiem tra chu so huu don hang: chan IDOR cho moi thao tac thanh toan. */
    @Transactional(readOnly = true)
    public Order requireOwnOrder(String username, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang #" + orderId));
        if (order.getCustomer() == null
                || !order.getCustomer().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Don hang #" + orderId + " khong thuoc tai khoan " + username);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public PaymentStrategy strategyOf(Order order) {
        return strategyRegistry.of(order.getPaymentMethod());
    }

    @Transactional(readOnly = true)
    public Payment latestPayment(Order order) {
        return paymentRepository.findTopByOrderOrderByIdDesc(order).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByRef(String transactionRef) {
        return Optional.ofNullable(resolveOrder(transactionRef));
    }

    /**
     * Bat dau mot lan thanh toan cho don hang.
     *
     * @param method phuong thuc khach chon o trang thanh toan; null = giu phuong thuc da dat
     */
    @Transactional
    public PaymentInitResult startPayment(String username, Long orderId, String clientIp,
                                          PaymentMethod method) {
        Order order = requireOwnOrder(username, orderId);

        if (method != null && method != order.getPaymentMethod()) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                throw new IllegalStateException("Don da thanh toan, khong doi duoc phuong thuc nua");
            }
            order.setPaymentMethod(method);
            orderRepository.save(order);
        }

        PaymentStrategy strategy = strategyRegistry.of(order.getPaymentMethod());
        if (!strategy.isConfigured()) {
            throw new IllegalStateException(strategy.displayName()
                    + " chua duoc cau hinh. Xem docs/BaoCao/Thien_CauHinh_VNPAY_MOMO.md");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Don nay da thanh toan roi");
        }
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Don nay da duoc hoan tien");
        }
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.RETURNED_REFUNDED) {
            throw new IllegalStateException("Don da ket thuc, khong thanh toan duoc nua");
        }

        PaymentInitResult result = strategy.initiate(order, clientIp);

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order).orElseGet(Payment::new);
        if (payment.getId() != null && payment.getStatus() == PaymentStatus.PAID) {
            // Don da tra tien mot lan roi thi moi lan tra sau la ban ghi moi.
            payment = new Payment();
        }
        payment.setOrder(order);
        payment.setMethod(order.getPaymentMethod());
        payment.setAmount(order.getTotal());
        payment.setTransactionId(result.transactionRef());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayResponse(catBot(result.message()));
        paymentRepository.save(payment);

        return result;
    }

    /**
     * Xu ly callback (khach quay ve) va IPN (cong goi server).
     *
     * Tra ve ket qua that bai ma KHONG doi gi khi: cong chua cau hinh, chu ky sai,
     * khong tim thay don, hoac so tien khong khop.
     */
    @Transactional
    public PaymentCallbackResult handleCallback(PaymentMethod method, Map<String, String> params) {
        PaymentStrategy strategy = strategyRegistry.of(method);

        if (!strategy.isConfigured()) {
            log.warn("Nhan callback {} nhung cong chua duoc cau hinh", method);
            return PaymentCallbackResult.failed("CHUA_CAU_HINH",
                    "Cong thanh toan " + method + " chua duoc cau hinh");
        }
        if (!strategy.verifySignature(params)) {
            log.warn("Callback {} co chu ky KHONG hop le, bo qua. Tham so: {}", method, params.keySet());
            return PaymentCallbackResult.failed("CHU_KY_KHONG_HOP_LE",
                    "Chu ky cua cong thanh toan khong hop le, khong cap nhat trang thai don");
        }

        PaymentCallbackResult result = strategy.parseCallback(params);
        Order order = resolveOrder(result.transactionRef());
        if (order == null) {
            log.warn("Callback {} khong tim thay don voi ma {}", method, result.transactionRef());
            return PaymentCallbackResult.failed("KHONG_TIM_THAY_DON",
                    "Khong tim thay don hang " + result.transactionRef());
        }

        if (result.amount() != null && order.getTotal() != null
                && result.amount().compareTo(order.getTotal()) != 0) {
            log.warn("Callback {} cho don {} bao so tien {} nhung don hang la {}",
                    method, order.getCode(), result.amount(), order.getTotal());
            return PaymentCallbackResult.failed("SAI_SO_TIEN",
                    "So tien cong bao khong khop voi don hang");
        }

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order).orElseGet(Payment::new);
        payment.setOrder(order);
        payment.setMethod(order.getPaymentMethod());
        payment.setAmount(order.getTotal());
        payment.setTransactionId(result.gatewayTransactionId() != null
                ? result.gatewayTransactionId()
                : result.transactionRef());
        payment.setGatewayResponse(catBot(result.responseCode() + " - " + result.message()));
        payment.setStatus(result.success() ? PaymentStatus.PAID : PaymentStatus.FAILED);
        if (result.success()) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        if (result.success()) {
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            orderStatusPublisher.publishOrderStatus(order,
                    "Don " + order.getCode() + " da thanh toan bang " + method.getLabel());
        } else if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
        }

        log.info("Xu ly callback {} cho don {}: thanh cong = {}", method, order.getCode(), result.success());
        return result;
    }

    /**
     * Tim don hang theo ma tham chieu cong tra ve.
     * MOMO gui dung ma don; VNPAY co the gui kem hau to, nen thu bo hau to sau dau '-'.
     */
    private Order resolveOrder(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return null;
        }
        Optional<Order> theoMa = orderRepository.findByCode(transactionRef);
        if (theoMa.isPresent()) {
            return theoMa.get();
        }
        int dauGach = transactionRef.lastIndexOf('-');
        if (dauGach > 0) {
            return orderRepository.findByCode(transactionRef.substring(0, dauGach)).orElse(null);
        }
        return null;
    }

    private String catBot(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_GATEWAY_RESPONSE ? value : value.substring(0, MAX_GATEWAY_RESPONSE);
    }
}