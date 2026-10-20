package com.utetra.module.catalog;

import com.utetra.entity.Category;
import com.utetra.entity.Product;
import com.utetra.entity.User;
import com.utetra.module.search.ProductSearchService;
import com.utetra.module.search.dto.ProductFilter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;

/**
 * Controller danh muc san pham, tim kiem/loc da tieu chi (Tuan 1) va chi tiet san pham (Tuan 2).
 * Luu y: Route /shop/{id} do ShopPublicController (module/vendor cua Thien) phu trach.
 */
@Controller
public class ProductCatalogController {

    private final ProductSearchService productSearchService;
    private final CatalogService catalogService;

    public ProductCatalogController(ProductSearchService productSearchService,
                                    CatalogService catalogService) {
        this.productSearchService = productSearchService;
        this.catalogService = catalogService;
    }

    /**
     * Trang danh sach san pham theo danh muc + bo loc gia + shop + sap xep + phan trang.
     */
    @GetMapping({"/products", "/search"})
    public String listProducts(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Long shopId,
                               @RequestParam(required = false) BigDecimal minPrice,
                               @RequestParam(required = false) BigDecimal maxPrice,
                               @RequestParam(defaultValue = "best-selling") String sort,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               Model model) {

        ProductFilter filter = new ProductFilter(keyword, categoryId, shopId, minPrice, maxPrice, sort);
        Page<Product> productPage = productSearchService.search(filter, page, size);

        model.addAttribute("page", productPage);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", catalogService.getAllActiveCategories());
        model.addAttribute("shops", catalogService.getActiveShops());
        model.addAttribute("selectedCategory", catalogService.findCategoryById(categoryId).orElse(null));
        model.addAttribute("selectedShop", catalogService.findShopById(shopId).orElse(null));

        return "product/list";
    }

    /**
     * Xem san pham theo slug danh muc (/category/tra-sua).
     */
    @GetMapping("/category/{slug}")
    public String listByCategorySlug(@PathVariable String slug,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long shopId,
                                     @RequestParam(required = false) BigDecimal minPrice,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     @RequestParam(defaultValue = "best-selling") String sort,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size,
                                     Model model) {
        Category category = catalogService.findCategoryBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay danh muc"));
        return listProducts(keyword, category.getId(), shopId, minPrice, maxPrice, sort, page, size, model);
    }

    /**
     * Trang chi tiet san pham (/products/{id}) — Tuan 2.
     */
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int reviewPage,
                                Principal principal,
                                Model model) {
        Product product = catalogService.findActiveProductById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham"));

        User currentUser = null;
        if (principal != null) {
            currentUser = catalogService.findUserByUsername(principal.getName()).orElse(null);
        }

        // Tang luot xem va luu vao ViewHistory neu user da dang nhap
        catalogService.recordProductView(product, currentUser);

        model.addAttribute("product", product);
        model.addAttribute("variants", catalogService.getVariants(product));
        model.addAttribute("toppings", catalogService.getShopToppings(product.getShop()));
        model.addAttribute("relatedProducts", catalogService.getRelatedProducts(product));
        model.addAttribute("reviewPage", catalogService.getProductReviews(product, reviewPage, 5));
        model.addAttribute("liked", catalogService.isProductLiked(currentUser, product));
        model.addAttribute("promotions", catalogService.getRunningPromotions(product.getShop()));

        return "product/detail";
    }
}
