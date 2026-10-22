package com.utetra.module.cart;

import com.utetra.entity.CartItem;
import com.utetra.entity.User;
import com.utetra.module.cart.dto.AddToCartRequest;
import com.utetra.module.cart.dto.CartSummaryDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controller gio hang luu tren CSDL (/cart).
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(Principal principal, Model model) {
        User user = cartService.getRequiredUser(principal.getName());
        CartSummaryDto summary = cartService.getCartSummary(user, null);
        model.addAttribute("cartSummary", summary);
        return "cart/index";
    }

    @PostMapping("/add")
    public String addToCart(@ModelAttribute AddToCartRequest request,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getRequiredUser(principal.getName());
            CartItem added = cartService.addToCart(user, request);
            if (request.isBuyNow()) {
                Long shopId = added.getProduct().getShop().getId();
                return "redirect:/checkout?shopId=" + shopId;
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã thêm " + added.getProduct().getName() + " vào giỏ hàng!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (request.getProductId() != null) {
                return "redirect:/products/" + request.getProductId();
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/update/{itemId}")
    public String updateQuantity(@PathVariable Long itemId,
                                 @RequestParam int quantity,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getRequiredUser(principal.getName());
            cartService.updateItemQuantity(user, itemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng món.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeItem(@PathVariable Long itemId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getRequiredUser(principal.getName());
            cartService.removeItem(user, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa món khỏi giỏ hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(Principal principal, RedirectAttributes redirectAttributes) {
        User user = cartService.getRequiredUser(principal.getName());
        cartService.clearCart(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đã làm trống giỏ hàng.");
        return "redirect:/cart";
    }
}
