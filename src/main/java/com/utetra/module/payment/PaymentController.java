package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.Payment;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Trang thanh toan cho khach (tuan 4).
 *
 *   GET  /payment/checkout/{orderId} : khach chon phuong thuc va bam "Thanh toan"
 *   POST /payment/start/{orderId}    : tao giao dich, chuyen sang cong VNPAY/MOMO
 *   GET  /payment/result             : xem ket qua sau khi cong tra ve
 *   GET  /payment/vnpay/return, /payment/momo/return : cong tra khach ve day
 *
 * Man hinh dat hang cua Hai chi can chuyen sang /payment/checkout/{orderId} la dung duoc.
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/checkout/{orderId}")
    public String checkout(@PathVariable Long orderId, Authentication authentication, Model model) {
        Order order = paymentService.requireOwnOrder(authentication.getName(), orderId);

        model.addAttribute("order", order);
        model.addAttribute("payment", paymentService.latestPayment(order));
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("strategy", paymentService.strategyOf(order));
        return "payment/checkout";
    }

    @PostMapping("/start/{orderId}")
    public String start(@PathVariable Long orderId,
                        @RequestParam(value = "method", required = false) String method,
                        Authentication authentication,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        try {
            PaymentInitResult result = paymentService.startPayment(authentication.getName(), orderId,
                    clientIp(request), parseMethod(method));

            if (result.requiresRedirect()) {
                return "redirect:" + result.redirectUrl();
            }
            redirectAttributes.addFlashAttribute("message", result.message());
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/payment/result?ref=" + result.transactionRef();

        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
            return "redirect:/payment/checkout/" + orderId;
        }
    }

    @GetMapping("/result")
    public String result(@RequestParam(value = "ref", required = false) String ref,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (ref == null || ref.isBlank()) {
            model.addAttribute("message", "Thieu ma don hang can xem ket qua");
            return "payment/result";
        }
        Order order = paymentService.findByRef(ref).orElse(null);
        if (order == null) {
            model.addAttribute("message", "Khong tim thay don hang " + ref);
            return "payment/result";
        }
        // Khong de nguoi khac xem duoc don khong phai cua minh.
        paymentService.requireOwnOrder(authentication.getName(), order.getId());

        Payment payment = paymentService.latestPayment(order);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        return "payment/result";
    }

    /** Cong VNPAY tra khach ve bang GET voi nhieu tham so. */
    @GetMapping("/vnpay/return")
    public String vnpayReturn(@RequestParam Map<String, String> params,
                              RedirectAttributes redirectAttributes) {
        return xuLyVaQuayVe(PaymentMethod.VNPAY, params, redirectAttributes);
    }

    /** Cong MOMO tra khach ve bang GET voi nhieu tham so. */
    @GetMapping("/momo/return")
    public String momoReturn(@RequestParam Map<String, String> params,
                             RedirectAttributes redirectAttributes) {
        return xuLyVaQuayVe(PaymentMethod.MOMO, params, redirectAttributes);
    }

    private String xuLyVaQuayVe(PaymentMethod method, Map<String, String> params,
                                RedirectAttributes redirectAttributes) {
        PaymentCallbackResult result = paymentService.handleCallback(method, params);
        redirectAttributes.addFlashAttribute("message", result.message());
        redirectAttributes.addFlashAttribute("success", result.success());

        String ref = result.transactionRef();
        if (ref == null || ref.isBlank()) {
            return "redirect:/payment/result";
        }
        return "redirect:/payment/result?ref=" + ref;
    }

    private PaymentMethod parseMethod(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}