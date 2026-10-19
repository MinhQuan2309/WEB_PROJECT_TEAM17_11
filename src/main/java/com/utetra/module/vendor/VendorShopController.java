package com.utetra.module.vendor;

import com.utetra.entity.Shop;
import com.utetra.module.vendor.dto.ShopUpdateRequest;
import com.utetra.module.vendor.upload.InvalidImageException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vendor quan ly trang chu shop cua minh (tuan 1).
 *
 * Duong dan co id de ro rang, nhung moi thao tac ghi deu qua
 * VendorShopService.requireOwnedShop(...) -> Vendor B vao /vendor/shop/{id-cua-A}
 * se bi 403, khong doc duoc cung khong sua duoc.
 */
@Controller
@RequestMapping("/vendor/shop")
public class VendorShopController {

    private final VendorShopService vendorShopService;

    public VendorShopController(VendorShopService vendorShopService) {
        this.vendorShopService = vendorShopService;
    }

    /** /vendor/shop -> chuyen ve dung shop cua nguoi dang nhap. */
    @GetMapping
    public String myShop(Authentication authentication) {
        Shop shop = vendorShopService.requireMyShop(authentication.getName());
        return "redirect:/vendor/shop/" + shop.getId();
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        Shop shop = vendorShopService.requireOwnedShop(authentication.getName(), id);
        if (!model.containsAttribute("form")) {
            ShopUpdateRequest form = new ShopUpdateRequest();
            form.setName(shop.getName());
            form.setDescription(shop.getDescription());
            form.setAddress(shop.getAddress());
            form.setPhone(shop.getPhone());
            model.addAttribute("form", form);
        }
        model.addAttribute("shop", shop);
        return "vendor/shop-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ShopUpdateRequest form,
                         BindingResult bindingResult,
                         @RequestParam(value = "logo", required = false) MultipartFile logo,
                         @RequestParam(value = "banner", required = false) MultipartFile banner,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("shop", vendorShopService.requireOwnedShop(authentication.getName(), id));
            return "vendor/shop-form";
        }

        try {
            vendorShopService.updateShop(authentication.getName(), id, form, logo, banner);
        } catch (InvalidImageException ex) {
            model.addAttribute("uploadError", ex.getMessage());
            model.addAttribute("shop", vendorShopService.requireOwnedShop(authentication.getName(), id));
            return "vendor/shop-form";
        }

        redirectAttributes.addFlashAttribute("message", "Da cap nhat thong tin shop");
        return "redirect:/vendor/shop/" + id;
    }
}