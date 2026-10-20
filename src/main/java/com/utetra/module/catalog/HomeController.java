package com.utetra.module.catalog;

import com.utetra.entity.Product;
import com.utetra.entity.User;
import com.utetra.module.catalog.dto.ProductBlockItemDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller trang chu cho ca Guest va User:
 * - Guest: hien thi >10 san pham cua cac shop, sap xep giam dan theo luot ban;
 *   co o tim kiem; bam mua -> chuyen sang dang nhap (/login).
 * - User (da dang nhap): hien thi 4 block 20 san pham (moi nhat / ban chay nhat /
 *   danh gia cao nhat / yeu thich nhieu nhat) co phan trang va lazy loading AJAX,
 *   kem danh sach san pham da xem gan day.
 */
@Controller
public class HomeController {

    private final CatalogService catalogService;

    public HomeController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"/", "/home"})
    public String home(@RequestParam(defaultValue = "0") int newestPage,
                       @RequestParam(defaultValue = "0") int bestPage,
                       @RequestParam(defaultValue = "0") int ratingPage,
                       @RequestParam(defaultValue = "0") int likedPage,
                       @RequestParam(defaultValue = "20") int size,
                       Principal principal,
                       Model model) {

        int blockSize = (size > 0 && size <= 40) ? size : CatalogService.USER_BLOCK_SIZE;

        // Danh sach >10 san pham ban chay giam dan cho Guest (12 san pham)
        List<Product> guestBestSellers = catalogService.getGuestBestSellers();
        model.addAttribute("products", guestBestSellers);
        model.addAttribute("categories", catalogService.getRootCategories());
        model.addAttribute("shops", catalogService.getActiveShops());

        // 4 block 20 san pham cho User (ho tro phan trang + lazy load)
        Page<Product> newestBlock = catalogService.getNewestProducts(newestPage, blockSize);
        Page<Product> bestSellingBlock = catalogService.getBestSellingProducts(bestPage, blockSize);
        Page<Product> topRatedBlock = catalogService.getTopRatedProducts(ratingPage, blockSize);
        Page<Product> mostLikedBlock = catalogService.getMostLikedProducts(likedPage, blockSize);

        model.addAttribute("newestBlock", newestBlock);
        model.addAttribute("bestSellingBlock", bestSellingBlock);
        model.addAttribute("topRatedBlock", topRatedBlock);
        model.addAttribute("mostLikedBlock", mostLikedBlock);
        model.addAttribute("blockSize", blockSize);

        if (principal != null) {
            User currentUser = catalogService.findUserByUsername(principal.getName()).orElse(null);
            if (currentUser != null) {
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("recentlyViewed", catalogService.getRecentlyViewed(currentUser));
                model.addAttribute("likedProductIds", catalogService.getLikedProductIds(currentUser));
            }
        }

        return "home/index";
    }

    /**
     * API phuc vu nut "Xem them (Lazy Loading)" cho 4 block tren trang chu User.
     */
    @GetMapping("/products/api/block")
    @ResponseBody
    public Map<String, Object> loadBlockPage(@RequestParam(defaultValue = "newest") String type,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<Product> productPage = catalogService.getBlockByType(type, page, size);
        List<ProductBlockItemDto> items = productPage.getContent().stream()
                .map(ProductBlockItemDto::from)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("page", productPage.getNumber());
        result.put("totalPages", productPage.getTotalPages());
        result.put("totalElements", productPage.getTotalElements());
        result.put("hasNext", productPage.hasNext());
        result.put("items", items);
        return result;
    }
}
