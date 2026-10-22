package com.utetra.module.admin;

import com.utetra.entity.DiscountType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Chuong trinh khuyen mai cua san: giam phan tram san pham hoac giam phi van chuyen. */
@Controller
@RequestMapping("/admin/promotions")
public class PromotionAdminController {

    private final PlatformPromotionService promotionService;

    public PromotionAdminController(PlatformPromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("now", LocalDateTime.now());
        return "admin/promotions";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam DiscountType discountType,
                       @RequestParam BigDecimal value,
                       @RequestParam(required = false) BigDecimal maxDiscount,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                       RedirectAttributes ra) {
        try {
            promotionService.save(id, name, description, discountType, value, maxDiscount, startAt, endAt);
            ra.addFlashAttribute("successMessage",
                    id == null ? "Da tao chuong trinh khuyen mai." : "Da cap nhat chuong trinh.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var p = promotionService.toggleActive(id);
            ra.addFlashAttribute("successMessage",
                    p.isActive() ? "Da bat chuong trinh." : "Da tat chuong trinh.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promotionService.delete(id);
            ra.addFlashAttribute("successMessage", "Da xoa chuong trinh.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}
