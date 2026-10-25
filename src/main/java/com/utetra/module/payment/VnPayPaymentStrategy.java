package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cong thanh toan VNPAY, ban 2.1.0 tren sandbox (tuan 4).
 *
 * Luong chay:
 *   1. initiate(): gom tham so vnp_*, sap xep theo ten, ky HMAC-SHA512 bang hashSecret,
 *      tra ve URL https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...&vnp_SecureHash=...
 *   2. Khach tra tien tren trang VNPAY, VNPAY goi ve:
 *        - Return URL (trinh duyet khach quay ve): /payment/vnpay/return
 *        - IPN URL (server goi server, nguon tin cay nhat): /payment/vnpay/ipn
 *   3. Ca hai deu phai qua verifySignature() truoc khi doi trang thai don.
 *
 * Luu y ve chu ky: chuoi du lieu ky la cac cap key=value DA MA HOA (RFC 3986) va
 * DA SAP XEP theo ten tham so. Luc tao URL va luc kiem tra phai dung y nguyen
 * cung mot ham (PaymentSignatures) - day la loi pho bien nhat khi tich hop VNPAY.
 */
@Component
public class VnPayPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(VnPayPaymentStrategy.class);

    private static final String SECURE_HASH = "vnp_SecureHash";
    private static final String SECURE_HASH_TYPE = "vnp_SecureHashType";
    private static final String SUCCESS_CODE = "00";
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentGatewayProperties properties;

    public VnPayPaymentStrategy(PaymentGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public String displayName() {
        return "VNPAY (sandbox)";
    }

    @Override
    public boolean isConfigured() {
        return properties.getVnpay().isConfigured();
    }

    @Override
    public PaymentInitResult initiate(Order order, String clientIp) {
        PaymentGatewayProperties.Vnpay config = properties.getVnpay();
        if (!config.isConfigured()) {
            throw new IllegalStateException(
                    "Chua cau hinh VNPAY: thieu app.payment.vnpay.tmn-code / hash-secret");
        }

        LocalDateTime now = LocalDateTime.now(VN);
        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", config.getVersion());
        params.put("vnp_Command", config.getCommand());
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Locale", config.getLocale());
        params.put("vnp_CurrCode", config.getCurrCode());
        params.put("vnp_TxnRef", order.getCode());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getCode());
        params.put("vnp_OrderType", config.getOrderType());
        params.put("vnp_Amount", String.valueOf(amountInVnpayUnit(order)));
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", now.plusMinutes(config.getExpireMinutes()).format(VNPAY_TIME));

        String hashData = PaymentSignatures.buildVnpayHashData(params);
        String secureHash = PaymentSignatures.hmacSha512(config.getHashSecret(), hashData);

        String url = config.getPayUrl() + "?" + hashData + "&" + SECURE_HASH + "=" + secureHash;
        log.info("Tao yeu cau thanh toan VNPAY cho don {} so tien {}", order.getCode(), order.getTotal());
        return PaymentInitResult.redirect(url, order.getCode(), "Chuyen sang cong thanh toan VNPAY");
    }

    /** VNPAY tinh so tien theo don vi nho nhat (VND x100). */
    private long amountInVnpayUnit(Order order) {
        BigDecimal total = order.getTotal() == null ? BigDecimal.ZERO : order.getTotal();
        return total.setScale(0, RoundingMode.HALF_UP).longValueExact() * 100L;
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get(SECURE_HASH);
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        TreeMap<String, String> sorted = PaymentSignatures.sortedParams(params, SECURE_HASH, SECURE_HASH_TYPE);
        String hashData = PaymentSignatures.buildVnpayHashData(sorted);
        String expected = PaymentSignatures.hmacSha512(properties.getVnpay().getHashSecret(), hashData);
        return PaymentSignatures.constantTimeEquals(expected, secureHash);
    }

    @Override
    public PaymentCallbackResult parseCallback(Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionRef = params.get("vnp_TxnRef");
        String gatewayTransactionId = params.get("vnp_TransactionNo");
        BigDecimal amount = parseAmount(params.get("vnp_Amount"));

        boolean success = SUCCESS_CODE.equals(responseCode)
                && (transactionStatus == null || SUCCESS_CODE.equals(transactionStatus));

        String message = success
                ? "VNPAY xac nhan thanh toan thanh cong"
                : "VNPAY bao loi, ma tra ve: " + responseCode;
        return new PaymentCallbackResult(success, transactionRef, gatewayTransactionId,
                responseCode, amount, message);
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        try {
            // VNPAY tra ve so tien da nhan 100.
            return new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}