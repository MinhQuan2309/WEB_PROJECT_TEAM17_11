package com.utetra.module.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Quan ly nha van chuyen: ten va phi van chuyen. */
@Controller
@RequestMapping("/admin/carriers")
public class CarrierAdminController {

    private final CarrierAdminService carrierAdminService;

    public CarrierAdminController(CarrierAdminService carrierAdminService) {
        this.carrierAdminService = carrierAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("carriers", carrierAdminService.findAll());
        return "admin/carriers";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam BigDecimal fee,
                       RedirectAttributes ra) {
        try {
            carrierAdminService.save(id, name, fee);
            ra.addFlashAttribute("successMessage",
                    id == null ? "Da them nha van chuyen." : "Da cap nhat nha van chuyen.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/carriers";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var c = carrierAdminService.toggleActive(id);
            ra.addFlashAttribute("successMessage",
                    c.isActive() ? "Da bat " + c.getName() : "Da tat " + c.getName());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/carriers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            carrierAdminService.delete(id);
            ra.addFlashAttribute("successMessage", "Da xoa nha van chuyen.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/carriers";
    }
}
