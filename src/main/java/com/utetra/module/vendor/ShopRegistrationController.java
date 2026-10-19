package com.utetra.module.vendor;

import com.utetra.entity.Shop;
import com.utetra.module.vendor.dto.ShopRegistrationRequest;
import com.utetra.module.vendor.upload.InvalidImageException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Dang ky shop (tuan 1).
 *
 * Vi sao dat o /shop/register chu khong phai /vendor/register:
 *   SecurityConfig gioi han /vendor/** chi cho VENDOR, ma nguoi dang ky shop con la USER.
 *   /shop/** la duong dan cong khai (khach xem shop), nen o day phai tu kiem tra
 *   dang nhap bang @PreAuthorize("isAuthenticated()").
 *
 * Sau khi gui form, shop o trang thai PENDING. Admin/Manager duyet -> ACTIVE va
 * nang role chu shop len VENDOR thi moi vao duoc khu /vendor.
 */
@Controller
@RequestMapping("/shop/register")
public class ShopRegistrationController {

    private final VendorShopService vendorShopService;

    public ShopRegistrationController(VendorShopService vendorShopService) {
        this.vendorShopService = vendorShopService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String form(Authentication authentication, Model model) {
        Optional<Shop> existing = vendorShopService.findMyShop(authentication.getName());
        if (existing.isPresent()) {
            model.addAttribute("shop", existing.get());
            return "shop/register-status";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ShopRegistrationRequest());
        }
        return "shop/register";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public String submit(@Valid @ModelAttribute("form") ShopRegistrationRequest form,
                         BindingResult bindingResult,
                         @RequestParam(value = "logo", required = false) MultipartFile logo,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "shop/register";
        }

        try {
            Shop shop = vendorShopService.registerShop(authentication.getName(), form, logo);
            redirectAttributes.addFlashAttribute("submittedShopName", shop.getName());
            return "redirect:/shop/register";
        } catch (InvalidImageException ex) {
            model.addAttribute("uploadError", ex.getMessage());
            return "shop/register";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
            return "redirect:/shop/register";
        }
    }
}