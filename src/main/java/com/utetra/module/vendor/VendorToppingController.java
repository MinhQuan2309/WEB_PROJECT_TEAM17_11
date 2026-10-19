package com.utetra.module.vendor;

import com.utetra.module.vendor.dto.ToppingForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quan ly topping cua shop (tuan 2).
 * Danh sach + form them nam cung mot trang cho gon, sua tung dong bang form nho.
 */
@Controller
@RequestMapping("/vendor/toppings")
public class VendorToppingController {

    private final VendorToppingService vendorToppingService;

    public VendorToppingController(VendorToppingService vendorToppingService) {
        this.vendorToppingService = vendorToppingService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("toppings", vendorToppingService.listMyToppings(authentication.getName()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ToppingForm());
        }
        return "vendor/toppings";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ToppingForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("toppings", vendorToppingService.listMyToppings(authentication.getName()));
            return "vendor/toppings";
        }
        vendorToppingService.create(authentication.getName(), form);
        redirectAttributes.addFlashAttribute("message", "Da them topping: " + form.getName());
        return "redirect:/vendor/toppings";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ToppingForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("formError", "Du lieu topping khong hop le");
            return "redirect:/vendor/toppings";
        }
        vendorToppingService.update(authentication.getName(), id, form);
        redirectAttributes.addFlashAttribute("message", "Da cap nhat topping");
        return "redirect:/vendor/toppings";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        var topping = vendorToppingService.toggleActive(authentication.getName(), id);
        redirectAttributes.addFlashAttribute("message",
                topping.isActive() ? "Da bat topping: " + topping.getName()
                                   : "Da tat topping: " + topping.getName());
        return "redirect:/vendor/toppings";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            vendorToppingService.delete(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("message", "Da xoa topping");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/vendor/toppings";
    }
}