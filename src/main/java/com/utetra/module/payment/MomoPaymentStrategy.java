package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cong thanh toan MOMO, API AIO v2 tren moi truong test (tuan 4).
 *
 * Luong chay:
 *   1. initiate(): POST JSON sang https://test-payment.momo.vn/v2/gateway/api/create
 *      voi chu ky HMAC-SHA256 de nhan payUrl, roi chuyen trinh duyet khach sang do.
 *   2. Khach tra tien trong vi MOMO, MOMO goi ve:
 *        - Return URL (khach quay ve): /payment/momo/return - GET
 *        - IPN URL    (server goi):    /payment/momo/ipn    - POST JSON
 *   3. Ca hai deu phai qua verifySignature() truoc khi doi trang thai don.
 *
 * Khac VNPAY mot diem quan trong: chuoi ky cua MOMO KHONG ma hoa gia tri, chi ghep
 * key=value&... theo thu tu ten tham so tang dan, va dung HMAC-SHA256.
 * Chuoi ky chi gom dung cac tham so trong tai lieu MOMO (danh sach duoi day), nho vay
 * du MOMO co tra them tham so khac thi chu ky van dung.
 */
@Component
public class MomoPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentStrategy.class);

    private static final String SUCCESS_CODE = "0";

    /** Cac tham so nam trong chuoi ky luc tao giao dich (thu tu tang dan theo ten). */
    private static final List<String> CREATE_FIELDS = List.of(
            "accessKey", "amount", "extraData", "ipnUrl", "orderId", "orderInfo",
            "partnerCode", "redirectUrl", "requestId", "requestType");

    /** Cac tham so nam trong chuoi ky cua du lieu MOMO tra ve (return URL va IPN). */
    private static final List<String> CALLBACK_FIELDS = List.of(
            "accessKey", "amount", "extraData", "message", "orderId", "orderInfo", "orderType",
            "partnerCode", "payType", "requestId", "responseTime", "resultCode", "transId");

    private final PaymentGatewayProperties properties;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public MomoPaymentStrategy(PaymentGatewayProperties properties) {
        this(properties, RestClient.create());
    }

    /** Cho phep truyen RestClient khac khi viet test. */
    MomoPaymentStrategy(PaymentGatewayProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO;
    }

    @Override
    public String displayName() {
        return "Vi MOMO (sandbox)";
    }

    @Override
    public boolean isConfigured() {
        return properties.getMomo().isConfigured();
    }

    @Override
    public PaymentInitResult initiate(Order order, String clientIp) {
        PaymentGatewayProperties.Momo config = properties.getMomo();
        if (!config.isConfigured()) {
            throw new IllegalStateException(
                    "Chua cau hinh MOMO: thieu app.payment.momo.partner-code / access-key / secret-key");
        }

        String requestId = order.getCode() + "-" + System.currentTimeMillis();
        long amount = order.getTotal() == null
                ? 0L
                : order.getTotal().setScale(0, RoundingMode.HALF_UP).longValueExact();
        String orderInfo = "Thanh toan don hang " + order.getCode();
        String extraData = "";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", config.getPartnerCode());
        body.put("partnerName", "UTETra");
        body.put("storeId", "UTETra");
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", order.getCode());
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", config.getReturnUrl());
        body.put("ipnUrl", config.getIpnUrl());
        body.put("lang", config.getLang());
        body.put("requestType", config.getRequestType());
        body.put("autoCapture", true);
        body.put("extraData", extraData);
        body.put("signature", signCreateRequest(order.getCode(), requestId, amount, orderInfo, extraData));

        try {
            Map<?, ?> response = restClient.post()
                    .uri(config.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("MOMO khong tra ve du lieu");
            }
            Object payUrl = response.get("payUrl");
            if (payUrl == null || String.valueOf(payUrl).isBlank()) {
                throw new IllegalStateException("MOMO tu choi tao giao dich: "
                        + response.get("resultCode") + " - " + response.get("message"));
            }
            log.info("Tao yeu cau thanh toan MOMO cho don {} so tien {}", order.getCode(), amount);
            return PaymentInitResult.redirect(String.valueOf(payUrl), order.getCode(),
                    "Chuyen sang vi MOMO");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Khong goi duoc cong MOMO: " + ex.getMessage(), ex);
        }
    }

    /** Chu ky cho request tao giao dich (ten tham so tang dan, gia tri khong ma hoa). */
    String signCreateRequest(String orderId, String requestId, long amount,
                             String orderInfo, String extraData) {
        PaymentGatewayProperties.Momo config = properties.getMomo();
        Map<String, String> giaTri = Map.of(
                "accessKey", config.getAccessKey(),
                "amount", String.valueOf(amount),
                "extraData", extraData == null ? "" : extraData,
                "ipnUrl", config.getIpnUrl(),
                "orderId", orderId,
                "orderInfo", orderInfo,
                "partnerCode", config.getPartnerCode(),
                "redirectUrl", config.getReturnUrl(),
                "requestId", requestId,
                "requestType", config.getRequestType());
        return PaymentSignatures.hmacSha256(config.getSecretKey(),
                PaymentSignatures.buildMomoRawData(select(CREATE_FIELDS, giaTri)));
    }

    /** Chu ky cho du lieu MOMO tra ve (return URL va IPN dung chung mot bo tham so). */
    String signCallback(Map<String, String> params) {
        PaymentGatewayProperties.Momo config = properties.getMomo();
        Map<String, String> giaTri = new java.util.HashMap<>(params == null ? Map.of() : params);
        giaTri.put("accessKey", config.getAccessKey());
        return PaymentSignatures.hmacSha256(config.getSecretKey(),
                PaymentSignatures.buildMomoRawData(select(CALLBACK_FIELDS, giaTri)));
    }

    /** Lay dung cac tham so trong danh sach, thieu thi coi la chuoi rong. */
    private TreeMap<String, String> select(List<String> fields, Map<String, String> nguon) {
        TreeMap<String, String> data = new TreeMap<>();
        for (String field : fields) {
            String value = nguon.get(field);
            data.put(field, value == null ? "" : value);
        }
        return data;
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String signature = params == null ? null : params.get("signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }
        return PaymentSignatures.constantTimeEquals(signCallback(params), signature);
    }

    @Override
    public PaymentCallbackResult parseCallback(Map<String, String> params) {
        String resultCode = params.get("resultCode");
        String orderId = params.get("orderId");
        String transId = params.get("transId");
        BigDecimal amount = parseAmount(params.get("amount"));
        boolean success = SUCCESS_CODE.equals(resultCode);

        String message = success
                ? "MOMO xac nhan thanh toan thanh cong"
                : "MOMO bao loi, ma tra ve: " + resultCode + " - " + params.getOrDefault("message", "");
        return new PaymentCallbackResult(success, orderId, transId, resultCode, amount, message);
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(rawAmount);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}