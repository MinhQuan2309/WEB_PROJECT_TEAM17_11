package com.utetra.module.checkout;

import com.utetra.entity.*;
import com.utetra.module.account.AccountService;
import com.utetra.module.cart.CartService;
import com.utetra.module.cart.dto.CartSummaryDto;
import com.utetra.module.checkout.dto.CheckoutRequest;
import com.utetra.module.checkout.dto.VoucherPreviewDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller thanh toan (/checkout): chon dia chi, nha van chuyen, ma giam gia Voucher
 * va phuong thuc thanh toan (COD / VNPAY / MOMO).
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final AccountService accountService;
    private final CheckoutService checkoutService;

    public CheckoutController(CartService cartService,
                              AccountService accountService,
                              CheckoutService checkoutService) {
        this.cartService = cartService;
        this.accountService = accountService;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public String checkoutPage(@RequestParam(required = false) Long shopId,
                               @RequestParam(required = false) String voucherCode,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = cartService.getRequiredUser(principal.getName());
        CartSummaryDto cartSummary = cartService.getCartSummary(user, shopId);
        if (cartSummary.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng đang trống, vui lòng chọn món trước khi thanh toán.");
            return "redirect:/cart";
        }

        List<Address> addresses = accountService.getAddresses(user);
        List<Carrier> carriers = checkoutService.getActiveCarriers();
        List<Voucher> availableVouchers = checkoutService.getAvailableVouchers(cartSummary);

        Long defaultCarrierId = !carriers.isEmpty() ? carriers.get(0).getId() : null;
        VoucherPreviewDto voucherPreview = checkoutService.previewVoucher(user, voucherCode, defaultCarrierId, shopId);

        model.addAttribute("user", user);
        model.addAttribute("shopId", shopId);
        model.addAttribute("cartSummary", cartSummary);
        model.addAttribute("addresses", addresses);
        model.addAttribute("carriers", carriers);
        model.addAttribute("availableVouchers", availableVouchers);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("voucherCode", voucherCode);
        model.addAttribute("voucherPreview", voucherPreview);

        return "checkout/index";
    }

    /**
     * API kiem tra nhanh ma giam gia Voucher bang AJAX ngay tren trang Checkout.
     */
    @GetMapping("/api/preview-voucher")
    @ResponseBody
    public VoucherPreviewDto previewVoucherApi(@RequestParam String code,
                                               @RequestParam(required = false) Long carrierId,
                                               @RequestParam(required = false) Long shopId,
                                               Principal principal) {
        User user = cartService.getRequiredUser(principal.getName());
        return checkoutService.previewVoucher(user, code, carrierId, shopId);
    }

    /**
     * Tao nhanh 3 Voucher mau de test luong ap ma giam gia.
     */
    @PostMapping("/seed-sample-vouchers")
    public String seedSampleVouchers(@RequestParam(required = false) Long shopId,
                                     RedirectAttributes redirectAttributes) {
        checkoutService.ensureSampleVouchersOnDemand();
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã kích hoạt 3 mã giảm giá dùng thử: UTETRA10 (Giảm 10%), GIAM15K (Giảm 15.000đ), FREESHIP (Miễn phí ship)!");
        return shopId != null ? "redirect:/checkout?shopId=" + shopId : "redirect:/checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(@ModelAttribute CheckoutRequest checkoutRequest,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getRequiredUser(principal.getName());
            List<Order> orders = checkoutService.placeOrders(user, checkoutRequest);
            String joinedCodes = orders.stream().map(Order::getCode).collect(Collectors.joining(","));

            // VNPAY da co khoa sandbox that nen di thang vao module payment cua Thien.
            // MOMO chua co khoa merchant nen van dung man hinh cong mo phong.
            if (checkoutRequest.getPaymentMethod() == PaymentMethod.VNPAY) {
                if (orders.size() > 1) {
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Giỏ hàng được tách thành " + orders.size() + " đơn. Thanh toán lần lượt"
                            + " từng đơn, các đơn còn lại xem trong Lịch sử mua hàng.");
                }
                return "redirect:/payment/checkout/" + orders.get(0).getId();
            }
            if (checkoutRequest.getPaymentMethod() == PaymentMethod.MOMO) {
                return "redirect:/checkout/payment-gateway?codes=" + joinedCodes
                        + "&method=" + checkoutRequest.getPaymentMethod().name();
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công! Mã đơn hàng: " + joinedCodes);
            return "redirect:/checkout/success?codes=" + joinedCodes;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (checkoutRequest.getShopId() != null) {
                return "redirect:/checkout?shopId=" + checkoutRequest.getShopId();
            }
            return "redirect:/checkout";
        }
    }

    /**
     * Man hinh cong thanh toan VNPAY / MOMO Sandbox tich hop de test va demo tron ven luong Online.
     */
    @GetMapping("/payment-gateway")
    public String paymentGateway(@RequestParam String codes,
                                 @RequestParam(defaultValue = "VNPAY") String method,
                                 Principal principal,
                                 Model model) {
        User user = cartService.getRequiredUser(principal.getName());
        List<String> codeList = Arrays.asList(codes.split(","));
        List<Order> orders = checkoutService.findUserOrdersByCodes(user, codeList);

        java.math.BigDecimal totalPay = orders.stream()
                .map(Order::getTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("orders", orders);
        model.addAttribute("codes", codes);
        model.addAttribute("method", method);
        model.addAttribute("totalPay", totalPay);
        return "checkout/payment-gateway";
    }

    @PostMapping("/payment-gateway/callback")
    public String paymentGatewayCallback(@RequestParam String codes,
                                         @RequestParam String method,
                                         @RequestParam(defaultValue = "true") boolean success,
                                         Principal principal,
                                         RedirectAttributes redirectAttributes) {
        User user = cartService.getRequiredUser(principal.getName());
        List<String> codeList = Arrays.asList(codes.split(","));
        checkoutService.confirmGatewayPayment(user, codeList, success, method);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán qua cổng " + method + " thành công cho đơn: " + codes);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Giao dịch qua cổng " + method + " đã bị hủy. Bạn có thể kiểm tra lại trong Lịch sử mua hàng.");
        }
        return "redirect:/checkout/success?codes=" + codes;
    }

    @GetMapping("/success")
    public String orderSuccess(@RequestParam String codes,
                               Principal principal,
                               Model model) {
        User user = cartService.getRequiredUser(principal.getName());
        List<String> codeList = Arrays.asList(codes.split(","));
        List<Order> orders = checkoutService.findUserOrdersByCodes(user, codeList);
        model.addAttribute("orders", orders);
        return "checkout/success";
    }
}
