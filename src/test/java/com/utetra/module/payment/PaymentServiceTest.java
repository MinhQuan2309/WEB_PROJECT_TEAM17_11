package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Payment;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shop;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.PaymentRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu thanh toan (tuan 4).
 *
 * Khoa sandbox duoc tiem bang @SpringBootTest(properties = ...) - khoa gia, khong phai
 * khoa that, va chi song trong luc chay test.
 */
@SpringBootTest(properties = {
        "app.payment.vnpay.tmn-code=TESTCODE",
        "app.payment.vnpay.hash-secret=SECRETKEYTEST",
        "app.payment.vnpay.return-url=http://localhost:8080/payment/vnpay/return",
        "app.payment.momo.partner-code=MOMO_PARTNER",
        "app.payment.momo.access-key=MOMO_ACCESS",
        "app.payment.momo.secret-key=MOMO_SECRET"
})
@Transactional
class PaymentServiceTest {

    @Autowired PaymentService paymentService;
    @Autowired PaymentGatewayProperties properties;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Order taoDon(PaymentMethod method, PaymentStatus paymentStatus, String total) {
        Shop shop = shopRepository.findByOwner(userRepository.findByUsername("vendor1").orElseThrow())
                .orElseThrow();
        Order order = new Order();
        order.setCode("PAY-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        order.setSubtotal(new BigDecimal(total));
        order.setShippingFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal(total));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    /** Tao du lieu callback VNPAY da ky dung nhu cong van lam. */
    private Map<String, String> callbackVnpay(String transactionRef, String soTienVnpay, String responseCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", responseCode);
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_TransactionNo", "14123456");
        params.put("vnp_Amount", soTienVnpay);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + transactionRef);
        params.put("vnp_BankCode", "NCB");

        String hashData = PaymentSignatures.buildVnpayHashData(PaymentSignatures.sortedParams(params));
        params.put("vnp_SecureHash",
                PaymentSignatures.hmacSha512(properties.getVnpay().getHashSecret(), hashData));
        return params;
    }

    @Test
    @DisplayName("COD: khong chuyen huong, chi ghi mot ban ghi thanh toan dang cho")
    void codKhongChuyenHuong() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PENDING, "65000");

        PaymentInitResult result = paymentService.startPayment("khach", order.getId(), "127.0.0.1", null);

        assertFalse(result.requiresRedirect());
        assertEquals(order.getCode(), result.transactionRef());

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(PaymentMethod.COD, payment.getMethod());
        assertEquals(0, new BigDecimal("65000").compareTo(payment.getAmount()));
    }

    @Test
    @DisplayName("Khach doi phuong thuc truoc khi tra tien: don duoc cap nhat va tra ve URL VNPAY")
    void doiPhuongThucTruocKhiTraTien() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PENDING, "65000");

        PaymentInitResult result = paymentService.startPayment("khach", order.getId(), "127.0.0.1",
                PaymentMethod.VNPAY);

        assertTrue(result.requiresRedirect());
        assertEquals(PaymentMethod.VNPAY, orderRepository.findById(order.getId()).orElseThrow().getPaymentMethod());
        assertTrue(result.redirectUrl().contains("vnp_SecureHash"));
    }

    @Test
    @DisplayName("IDOR: khach khac khong bat dau thanh toan duoc cho don nguoi khac")
    void khongThanhToanHoDonNguoiKhac() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PENDING, "65000");

        assertThrows(AccessDeniedException.class,
                () -> paymentService.startPayment("vendor1", order.getId(), "127.0.0.1", null));
        assertThrows(AccessDeniedException.class,
                () -> paymentService.requireOwnOrder("vendor1", order.getId()));
    }

    @Test
    @DisplayName("Don da thanh toan thi khong thanh toan lai")
    void donDaThanhToanKhongTraLai() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PAID, "65000");
        assertThrows(IllegalStateException.class,
                () -> paymentService.startPayment("khach", order.getId(), "127.0.0.1", null));
    }

    @Test
    @DisplayName("Don da huy thi khong thanh toan duoc")
    void donDaHuyKhongThanhToanDuoc() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PENDING, "65000");
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        assertThrows(IllegalStateException.class,
                () -> paymentService.startPayment("khach", order.getId(), "127.0.0.1", null));
    }

    @Test
    @DisplayName("Callback chu ky SAI thi khong doi gi ca")
    void callbackSaiChuKyKhongDoiGi() {
        Order order = taoDon(PaymentMethod.VNPAY, PaymentStatus.PENDING, "65000");
        Map<String, String> params = callbackVnpay(order.getCode(), "6500000", "00");
        params.put("vnp_SecureHash", "chu-ky-bia-dat");

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY, params);

        assertFalse(result.success());
        assertEquals("CHU_KY_KHONG_HOP_LE", result.responseCode());
        assertEquals(PaymentStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus(),
                "Chu ky sai thi don phai giu nguyen trang thai");
        assertTrue(paymentRepository.findByOrder(order).stream()
                .noneMatch(p -> p.getStatus() == PaymentStatus.PAID));
    }

    @Test
    @DisplayName("Callback chu ky dung: don va ban ghi thanh toan chuyen sang da tra tien")
    void callbackDungChuKyCapNhatTrangThai() {
        Order order = taoDon(PaymentMethod.VNPAY, PaymentStatus.PENDING, "65000");
        paymentService.startPayment("khach", order.getId(), "127.0.0.1", PaymentMethod.VNPAY);

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY,
                callbackVnpay(order.getCode(), "6500000", "00"));

        assertTrue(result.success());
        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, sau.getPaymentStatus());

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(sau).orElseThrow();
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals("14123456", payment.getTransactionId());
        assertNotNull(payment.getPaidAt());
    }

    @Test
    @DisplayName("Callback bao that bai: don chuyen sang thanh toan that bai, khach tra lai duoc")
    void callbackThatBaiThiChoTraLai() {
        Order order = taoDon(PaymentMethod.VNPAY, PaymentStatus.PENDING, "65000");
        paymentService.startPayment("khach", order.getId(), "127.0.0.1", PaymentMethod.VNPAY);

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY,
                callbackVnpay(order.getCode(), "6500000", "24"));

        assertFalse(result.success());
        assertEquals(PaymentStatus.FAILED,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
        assertEquals(PaymentStatus.FAILED,
                paymentRepository.findTopByOrderOrderByIdDesc(order).orElseThrow().getStatus());

        // Khach duoc phep thu lai.
        PaymentInitResult lai = paymentService.startPayment("khach", order.getId(), "127.0.0.1", null);
        assertTrue(lai.requiresRedirect());
    }

    @Test
    @DisplayName("Callback dung chu ky nhung SAI SO TIEN thi khong cap nhat")
    void callbackSaiSoTienKhongCapNhat() {
        Order order = taoDon(PaymentMethod.VNPAY, PaymentStatus.PENDING, "65000");

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY,
                callbackVnpay(order.getCode(), "100", "00"));

        assertFalse(result.success());
        assertEquals("SAI_SO_TIEN", result.responseCode());
        assertEquals(PaymentStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("Callback dung chu ky nhung khong co don tuong ung")
    void callbackKhongCoDon() {
        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY,
                callbackVnpay("KHONG-CO-DON-NAY", "6500000", "00"));

        assertFalse(result.success());
        assertEquals("KHONG_TIM_THAY_DON", result.responseCode());
    }

    @Test
    @DisplayName("Ma don VNPAY co hau to van tim dung don (bo hau to sau dau gach)")
    void maDonCoHauToVanTimDungDon() {
        Order order = taoDon(PaymentMethod.VNPAY, PaymentStatus.PENDING, "65000");
        paymentService.startPayment("khach", order.getId(), "127.0.0.1", PaymentMethod.VNPAY);

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY,
                callbackVnpay(order.getCode() + "-1699000000", "6500000", "00"));

        assertTrue(result.success());
        assertEquals(PaymentStatus.PAID,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("COD khong co callback: khong the gia mao de danh dau da tra tien")
    void codKhongCoCallback() {
        Order order = taoDon(PaymentMethod.COD, PaymentStatus.PENDING, "65000");

        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.COD,
                Map.of("vnp_ResponseCode", "00"));

        assertFalse(result.success());
        assertEquals(PaymentStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }
}