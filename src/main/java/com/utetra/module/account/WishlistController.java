package com.utetra.module.account;

import com.utetra.entity.User;
import com.utetra.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Dieu huong man hinh san pham yeu thich (/wishlist) va lich su san pham da xem (tuan 5).
 */
@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private static final int PAGE_SIZE = 12;

    private final AccountService accountService;

    public WishlistController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Hien thi danh sach san pham yeu thich va 10 san pham vua xem gan nhat. */
    @GetMapping
    public String viewWishlist(@RequestParam(defaultValue = "0") int page,
                               Principal principal,
                               Model model) {
        User user = accountService.getRequiredUser(principal.getName());
        Page<Wishlist> wishlistPage = accountService.getWishlistPage(user, page, PAGE_SIZE);

        model.addAttribute("wishlistPage", wishlistPage);
        model.addAttribute("recentlyViewed", accountService.getRecentlyViewed(user));
        return "account/wishlist";
    }

    /** Them hoac bo yeu thich mot san pham, cap nhat bo dem likeCount. */
    @PostMapping("/toggle/{productId}")
    public String toggleWishlist(@PathVariable Long productId,
                                 @RequestParam(required = false) String redirectUrl,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            boolean liked = accountService.toggleWishlist(user, productId);
            redirectAttributes.addFlashAttribute("successMessage",
                    liked ? "Đã thêm sản phẩm vào danh sách yêu thích." : "Đã bỏ sản phẩm khỏi danh sách yêu thích.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/wishlist";
    }
}
