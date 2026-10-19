package com.utetra.module.catalog;

import com.utetra.entity.*;
import com.utetra.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service xu ly nghiep vu trang chu Guest/User, danh muc, chi tiet san pham va lich su xem.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int GUEST_HOME_SIZE = 12; // > 10 san pham theo yeu cau de tai
    public static final int USER_BLOCK_SIZE = 20; // 4 block 20 san pham theo yeu cau de tai

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ToppingRepository toppingRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;

    public CatalogService(ProductRepository productRepository,
                          ProductVariantRepository variantRepository,
                          ToppingRepository toppingRepository,
                          CategoryRepository categoryRepository,
                          ShopRepository shopRepository,
                          ReviewRepository reviewRepository,
                          WishlistRepository wishlistRepository,
                          ViewHistoryRepository viewHistoryRepository,
                          PromotionRepository promotionRepository,
                          VoucherRepository voucherRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.toppingRepository = toppingRepository;
        this.categoryRepository = categoryRepository;
        this.shopRepository = shopRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistRepository = wishlistRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.promotionRepository = promotionRepository;
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
    }

    /**
     * Guest trang chu: hien thi TREN 10 san pham (12 san pham) cua cac shop,
     * sap xep giam dan theo luot ban (soldCount).
     */
    public List<Product> getGuestBestSellers() {
        Page<Product> page = productRepository.findByActiveTrueOrderBySoldCountDesc(
                PageRequest.of(0, GUEST_HOME_SIZE));
        return page.getContent();
    }

    /**
     * User trang chu: 4 block 20 san pham (moi nhat, ban chay nhat, danh gia cao nhat, yeu thich nhieu nhat).
     */
    public Page<Product> getNewestProducts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : USER_BLOCK_SIZE);
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
    }

    public Page<Product> getBestSellingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : USER_BLOCK_SIZE);
        return productRepository.findByActiveTrueOrderBySoldCountDesc(pageable);
    }

    public Page<Product> getTopRatedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : USER_BLOCK_SIZE);
        return productRepository.findByActiveTrueOrderByRatingAvgDescRatingCountDesc(pageable);
    }

    public Page<Product> getMostLikedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : USER_BLOCK_SIZE);
        return productRepository.findByActiveTrueOrderByLikeCountDesc(pageable);
    }

    public Page<Product> getBlockByType(String type, int page, int size) {
        if (type == null) {
            return getNewestProducts(page, size);
        }
        return switch (type) {
            case "best-selling" -> getBestSellingProducts(page, size);
            case "top-rated" -> getTopRatedProducts(page, size);
            case "most-liked" -> getMostLikedProducts(page, size);
            default -> getNewestProducts(page, size);
        };
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrue();
    }

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    public Optional<Category> findCategoryById(Long id) {
        if (id == null) return Optional.empty();
        return categoryRepository.findById(id);
    }

    public Optional<Category> findCategoryBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return categoryRepository.findBySlug(slug);
    }

    public List<Shop> getActiveShops() {
        return shopRepository.findByStatus(ShopStatus.ACTIVE);
    }

    public Optional<Shop> findShopById(Long id) {
        if (id == null) return Optional.empty();
        return shopRepository.findById(id);
    }

    public Optional<Product> findActiveProductById(Long id) {
        return productRepository.findById(id).filter(Product::isActive);
    }

    public List<ProductVariant> getVariants(Product product) {
        return variantRepository.findByProduct(product);
    }

    public List<Topping> getShopToppings(Shop shop) {
        if (shop == null) return Collections.emptyList();
        return toppingRepository.findByShopAndActiveTrue(shop);
    }

    public List<Product> getRelatedProducts(Product product) {
        if (product.getCategory() != null) {
            return productRepository.findByCategoryAndActiveTrue(
                            product.getCategory(), PageRequest.of(0, 6))
                    .getContent()
                    .stream()
                    .filter(p -> !p.getId().equals(product.getId()))
                    .limit(5)
                    .toList();
        }
        return productRepository.findTop10ByActiveTrueOrderBySoldCountDesc()
                .stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .limit(5)
                .toList();
    }

    public Page<Review> getProductReviews(Product product, int page, int size) {
        return reviewRepository.findByProductOrderByIdDesc(
                product, PageRequest.of(Math.max(page, 0), size > 0 ? size : 5));
    }

    public List<Promotion> getRunningPromotions(Shop shop) {
        return promotionRepository.findRunning(LocalDateTime.now(), shop);
    }

    public List<Voucher> getAvailableVouchersForShop(Shop shop) {
        if (shop == null) {
            return voucherRepository.findByShopIsNullAndActiveTrue();
        }
        return voucherRepository.findByShopAndActiveTrue(shop);
    }

    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUsername(username);
    }

    public List<ViewHistory> getRecentlyViewed(User user) {
        if (user == null) return Collections.emptyList();
        return viewHistoryRepository.findTop10ByUserOrderByViewedAtDesc(user);
    }

    public boolean isProductLiked(User user, Product product) {
        if (user == null || product == null) return false;
        return wishlistRepository.existsByUserAndProduct(user, product);
    }

    public Set<Long> getLikedProductIds(User user) {
        if (user == null) return Collections.emptySet();
        return wishlistRepository.findByUserOrderByIdDesc(user, PageRequest.of(0, 200))
                .getContent()
                .stream()
                .map(w -> w.getProduct().getId())
                .collect(Collectors.toSet());
    }

    @Transactional
    public void recordProductView(Product product, User user) {
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        if (user != null) {
            ViewHistory history = viewHistoryRepository.findByUserAndProduct(user, product)
                    .orElseGet(() -> {
                        ViewHistory vh = new ViewHistory();
                        vh.setUser(user);
                        vh.setProduct(product);
                        return vh;
                    });
            history.setViewedAt(LocalDateTime.now());
            viewHistoryRepository.save(history);
        }
    }
}
