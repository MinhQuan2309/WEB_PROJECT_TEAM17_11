package com.utetra.module.vendor;

import com.utetra.entity.DiscountType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Khuyen mai cua shop. SecurityConfig da gioi han /vendor/** cho VENDOR.
 */
@Controller
@RequestMapping("/vendor/promotions")
public class VendorPromotionController {

    private final VendorPromotionService promotionService;

    public VendorPromotionController(VendorPromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("promotions", promotionService.promotionsOf(username));
        model.addAttribute("vouchers", promotionService.vouchersOf(username));
        model.addAttribute("products", promotionService.productsOfMyShop(username));
        model.addAttribute("discountTypes", DiscountType.values());
        return "vendor/promotions";
    }

    // ------------------------------------------------------ chuong trinh

    @PostMapping("/save")
    public String savePromotion(@RequestParam(required = false) Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam DiscountType discountType,
                                @RequestParam BigDecimal value,
                                @RequestParam(required = false) BigDecimal maxDiscount,
                                @RequestParam(required = false) Long productId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            promotionService.savePromotion(principal.getName(), id, name, description,
                    discountType, value, maxDiscount, productId, startAt, endAt);
            return id == null ? "Da tao chuong trinh khuyen mai." : "Da cap nhat chuong trinh.";
        });
    }

    @PostMapping("/{id}/toggle")
    public String togglePromotion(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            var p = promotionService.togglePromotion(principal.getName(), id);
            return p.isActive() ? "Da bat chuong trinh." : "Da tat chuong trinh.";
        });
    }

    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            promotionService.deletePromotion(principal.getName(), id);
            return "Da xoa chuong trinh.";
        });
    }

    // ---------------------------------------------------------- ma giam gia

    @PostMapping("/vouchers/save")
    public String saveVoucher(@RequestParam(required = false) Long id,
                              @RequestParam String code,
                              @RequestParam DiscountType discountType,
                              @RequestParam BigDecimal value,
                              @RequestParam(required = false) BigDecimal minOrderTotal,
                              @RequestParam(required = false) Integer quantity,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                              Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            var v = promotionService.saveVoucher(principal.getName(), id, code, discountType,
                    value, minOrderTotal, quantity, startAt, endAt);
            return "Da luu ma giam gia " + v.getCode() + ".";
        });
    }

    @PostMapping("/vouchers/{id}/toggle")
    public String toggleVoucher(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            var v = promotionService.toggleVoucher(principal.getName(), id);
            return v.isActive() ? "Da bat ma " + v.getCode() + "." : "Da tat ma " + v.getCode() + ".";
        });
    }

    @PostMapping("/vouchers/{id}/delete")
    public String deleteVoucher(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> {
            promotionService.deleteVoucher(principal.getName(), id);
            return "Da xoa ma giam gia.";
        });
    }

    /** Gom try/catch vao mot cho de sau handler khong lap lai. */
    private String run(RedirectAttributes ra, Supplier<String> action) {
        try {
            ra.addFlashAttribute("successMessage", action.get());
        } catch (AccessDeniedException ex) {
            ra.addFlashAttribute("errorMessage", "Khong co quyen: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vendor/promotions";
    }
}
