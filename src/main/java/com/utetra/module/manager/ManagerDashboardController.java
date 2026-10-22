package com.utetra.module.manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Khu Manager: dashboard, chu shop va san pham trong pham vi phu trach.
 * Viec duyet shop nam o ShopManagerController.
 */
@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    private final ManagerScopeService scopeService;

    public ManagerDashboardController(ManagerScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        var actor = scopeService.requireActor(principal.getName());
        model.addAttribute("summary", scopeService.summaryOf(actor));
        model.addAttribute("shops", scopeService.shopsOf(actor));
        return "manager/dashboard";
    }

    @GetMapping("/users")
    public String owners(Principal principal, Model model) {
        var actor = scopeService.requireActor(principal.getName());
        model.addAttribute("owners", scopeService.shopOwnersOf(actor));
        model.addAttribute("shops", scopeService.shopsOf(actor));
        return "manager/users";
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page,
                           Principal principal, Model model) {
        var actor = scopeService.requireActor(principal.getName());
        model.addAttribute("page", scopeService.productsOf(actor, page));
        return "manager/products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggleProduct(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            var actor = scopeService.requireActor(principal.getName());
            var product = scopeService.toggleProductActive(actor, id);
            ra.addFlashAttribute("successMessage", product.isActive()
                    ? "Da hien lai san pham " + product.getName()
                    : "Da an san pham " + product.getName());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            ra.addFlashAttribute("errorMessage", "Khong co quyen: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/products";
    }
}
