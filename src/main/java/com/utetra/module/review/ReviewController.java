package com.utetra.module.review;

import com.utetra.entity.Order;
import com.utetra.entity.Product;
import com.utetra.entity.User;
import com.utetra.module.review.dto.ReviewFormRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Dieu huong man hinh viet danh gia 1-5 sao va binh luan kem anh/video (/review).
 */
@Controller
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Hien thi form danh gia cho san pham thuoc don hang da giao. */
    @GetMapping("/product/{productId}")
    public String reviewForm(@PathVariable Long productId,
                             @RequestParam(required = false) Long orderId,
                             Principal principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = reviewService.getRequiredUser(principal.getName());
        Product product = reviewService.getRequiredProduct(productId);
        List<Order> deliveredOrders = reviewService.findDeliveredOrdersWithProduct(user, product);

        if (deliveredOrders.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và đơn hàng ở trạng thái Đã giao.");
            return "redirect:/products/" + productId;
        }

        ReviewFormRequest form = new ReviewFormRequest();
        form.setProductId(productId);
        form.setOrderId(orderId != null ? orderId : deliveredOrders.get(0).getId());
        form.setRating(5);

        model.addAttribute("product", product);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("reviewForm", form);
        model.addAttribute("cloudinaryReady", reviewService.isCloudinaryReady());
        return "review/form";
    }

    /** Nhan du lieu danh gia, binh luan va file anh/video tu khach hang. */
    @PostMapping("/submit")
    public String submitReview(@ModelAttribute ReviewFormRequest reviewForm,
                               @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = reviewService.getRequiredUser(principal.getName());
            reviewService.createReview(user, reviewForm, mediaFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cảm ơn bạn đã gửi đánh giá và bình luận cho sản phẩm!");
            return "redirect:/products/" + reviewForm.getProductId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (reviewForm.getProductId() != null) {
                return "redirect:/review/product/" + reviewForm.getProductId()
                        + (reviewForm.getOrderId() != null ? "?orderId=" + reviewForm.getOrderId() : "");
            }
            return "redirect:/orders";
        }
    }
}
