package com.utetra.module.payment;

import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IPN - cong thanh toan goi thang vao server (tuan 4).
 *
 * Day la nguon tin cay nhat de doi trang thai don: khach co the tat trinh duyet sau khi
 * tra tien nen Return URL khong phai luc nao cung duoc goi.
 *
 *   GET  /payment/vnpay/ipn : VNPAY goi, phai tra JSON {"RspCode":"00","Message":"..."}
 *   POST /payment/momo/ipn  : MOMO goi JSON, tra 204 la da nhan
 *
 * Ca hai deu di qua PaymentService.handleCallback nen bat buoc phai dung chu ky.
 */
@RestController
@RequestMapping("/payment")
public class PaymentIpnController {

    private static final Logger log = LoggerFactory.getLogger(PaymentIpnController.class);

    private final PaymentService paymentService;

    public PaymentIpnController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.VNPAY, params);
        return ResponseEntity.ok(vnpayResponse(result));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody(required = false) Map<String, Object> body) {
        PaymentCallbackResult result = paymentService.handleCallback(PaymentMethod.MOMO, toStringMap(body));
        if (!result.success() && "CHU_KY_KHONG_HOP_LE".equals(result.responseCode())) {
            // Chu ky sai: khong xac nhan de cong biet ma gui lai / bao dong.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.noContent().build();
    }

    /** Ma tra loi theo tai lieu VNPAY: 00 thanh cong, 97 sai chu ky, 01 khong thay don... */
    private Map<String, String> vnpayResponse(PaymentCallbackResult result) {
        Map<String, String> body = new LinkedHashMap<>();
        if (result.success()) {
            body.put("RspCode", "00");
            body.put("Message", "Confirm Success");
            return body;
        }
        switch (result.responseCode() == null ? "" : result.responseCode()) {
            case "CHU_KY_KHONG_HOP_LE" -> {
                body.put("RspCode", "97");
                body.put("Message", "Invalid signature");
            }
            case "KHONG_TIM_THAY_DON" -> {
                body.put("RspCode", "01");
                body.put("Message", "Order not found");
            }
            case "SAI_SO_TIEN" -> {
                body.put("RspCode", "04");
                body.put("Message", "Invalid amount");
            }
            default -> {
                log.info("VNPAY IPN tra ve that bai: {}", result.message());
                body.put("RspCode", "00");
                body.put("Message", "Confirm Success");
            }
        }
        return body;
    }

    private Map<String, String> toStringMap(Map<String, Object> body) {
        Map<String, String> params = new LinkedHashMap<>();
        if (body != null) {
            body.forEach((key, value) -> params.put(key, value == null ? null : String.valueOf(value)));
        }
        return params;
    }
}