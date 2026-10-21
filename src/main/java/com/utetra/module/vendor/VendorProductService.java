package com.utetra.module.vendor;

import com.utetra.entity.Category;
import com.utetra.entity.Product;
import com.utetra.entity.ProductVariant;
import com.utetra.entity.Shop;
import com.utetra.module.media.CloudinaryService;
import com.utetra.module.vendor.dto.ProductForm;
import com.utetra.module.vendor.dto.VariantForm;
import com.utetra.module.vendor.upload.ImageUploadValidator;
import com.utetra.repository.CartItemRepository;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import com.utetra.repository.ReviewRepository;
import com.utetra.repository.WishlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiep vu san pham cua shop (tuan 2): CRUD san pham + size + anh Cloudinary.
 *
 * Hai diem can luu y khi doc code nay:
 *   1. Moi thao tac ghi deu qua requireOwnedProduct(...) -> chan IDOR: Vendor B khong
 *      sua / xoa duoc san pham cua Vendor A du biet id.
 *   2. Xoa cung chi thuc hien duoc khi san pham chua bi tham chieu o bat ky dau
 *      (don hang, gio hang, yeu thich, danh gia). Da ban roi thi chi tat ban,
 *      de lich su don hang cua khach khong bi vo.
 */
@Service
public class VendorProductService {

    /** Thu muc tren Cloudinary cho anh san pham. */
    public static final String PRODUCT_IMAGE_FOLDER = "products";

    /** Chan form gui len qua nhieu size trong mot lan. */
    private static final int MAX_VARIANTS = 10;

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final VendorShopService vendorShopService;
    private final CloudinaryService cloudinaryService;
    private final ImageUploadValidator imageUploadValidator;

    public VendorProductService(ProductRepository productRepository,
                                ProductVariantRepository variantRepository,
                                CategoryRepository categoryRepository,
                                OrderItemRepository orderItemRepository,
                                CartItemRepository cartItemRepository,
                                WishlistRepository wishlistRepository,
                                ReviewRepository reviewRepository,
                                VendorShopService vendorShopService,
                                CloudinaryService cloudinaryService,
                                ImageUploadValidator imageUploadValidator) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistRepository = wishlistRepository;
        this.reviewRepository = reviewRepository;
        this.vendorShopService = vendorShopService;
        this.cloudinaryService = cloudinaryService;
        this.imageUploadValidator = imageUploadValidator;
    }

    /** Danh sach san pham cua shop minh. active = null nghia la lay ca san pham dang an. */
    @Transactional(readOnly = true)
    public Page<Product> listMyProducts(String username, Boolean active, int page, int size) {
        Shop shop = vendorShopService.requireMyShop(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        if (active == null) {
            return productRepository.findByShopOrderByIdDesc(shop, pageable);
        }
        return productRepository.findByShopAndActiveOrderByIdDesc(shop, active, pageable);
    }

    /** Diem kiem tra chu so huu san pham, dung lai cho sua / xoa / doi trang thai ban. */
    @Transactional(readOnly = true)
    public Product requireOwnedProduct(String username, Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham #" + productId));
        if (product.getShop() == null
                || product.getShop().getOwner() == null
                || !product.getShop().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "San pham #" + productId + " khong thuoc shop cua tai khoan " + username);
        }
        return product;
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> variantsOf(Product product) {
        return variantRepository.findByProductOrderByIdAsc(product);
    }

    @Transactional
    public Product create(String username, ProductForm form, MultipartFile image) {
        Shop shop = vendorShopService.requireMyShop(username);

        Product product = new Product();
        product.setShop(shop);
        applyForm(product, form);
        if (imageUploadValidator.validateIfPresent(image)) {
            product.setImageUrl(cloudinaryService.upload(image, PRODUCT_IMAGE_FOLDER));
        }

        Product saved = productRepository.save(product);
        syncVariants(saved, form.getVariants());
        return saved;
    }

    @Transactional
    public Product update(String username, Long productId, ProductForm form, MultipartFile image) {
        Product product = requireOwnedProduct(username, productId);

        applyForm(product, form);
        if (imageUploadValidator.validateIfPresent(image)) {
            product.setImageUrl(cloudinaryService.upload(image, PRODUCT_IMAGE_FOLDER));
        }

        Product saved = productRepository.save(product);
        syncVariants(saved, form.getVariants());
        return saved;
    }

    /** Bat / tat ban. San pham dang an se khong hien o trang khach. */
    @Transactional
    public Product toggleActive(String username, Long productId) {
        Product product = requireOwnedProduct(username, productId);
        product.setActive(!product.isActive());
        return productRepository.save(product);
    }

    /**
     * Xoa cung san pham. Chi thanh cong khi san pham chua bi tham chieu o dau.
     * Da ban roi thi phai dung chuc nang tat ban de giu lich su don hang.
     */
    @Transactional
    public void delete(String username, Long productId) {
        Product product = requireOwnedProduct(username, productId);

        if (orderItemRepository.existsByProduct(product)) {
            throw new IllegalStateException(
                    "San pham da nam trong don hang nen khong xoa duoc. Hay dung chuc nang Tat ban.");
        }
        if (cartItemRepository.existsByProduct(product)
                || wishlistRepository.countByProduct(product) > 0
                || reviewRepository.countByProduct(product) > 0) {
            throw new IllegalStateException(
                    "San pham dang duoc khach hang tham chieu (gio hang / yeu thich / danh gia) "
                    + "nen khong xoa duoc. Hay dung chuc nang Tat ban.");
        }

        List<ProductVariant> variants = variantRepository.findByProductOrderByIdAsc(product);
        for (ProductVariant variant : variants) {
            if (cartItemRepository.existsByVariant(variant)) {
                throw new IllegalStateException(
                        "Size \"" + variant.getName() + "\" dang co trong gio hang cua khach nen khong xoa duoc.");
            }
        }

        variantRepository.deleteAll(variants);
        productRepository.delete(product);
    }

    private void applyForm(Product product, ProductForm form) {
        product.setName(form.getName().trim());
        product.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        product.setBasePrice(form.getBasePrice() == null ? BigDecimal.ZERO : form.getBasePrice());
        product.setActive(form.isActive());
        product.setCategory(resolveCategory(form.getCategoryId()));
    }

    /** Chi nhan danh muc dang hoat dong; id la khong ton tai thi bo qua (khong nem loi). */
    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElse(null);
    }

    /**
     * Dong bo danh sach size theo form: sua tai cho cac dong tuong ung, them dong moi,
     * xoa cac dong bi bo khoi form (neu chua bi gio hang tham chieu).
     */
    private void syncVariants(Product product, List<VariantForm> rows) {
        List<VariantForm> hopLe = new ArrayList<>();
        if (rows != null) {
            for (VariantForm row : rows) {
                if (row != null && row.getName() != null && !row.getName().isBlank()) {
                    hopLe.add(row);
                }
            }
        }
        if (hopLe.size() > MAX_VARIANTS) {
            hopLe = hopLe.subList(0, MAX_VARIANTS);
        }

        List<ProductVariant> hienCo = variantRepository.findByProductOrderByIdAsc(product);
        int soDongGiuLai = Math.min(hopLe.size(), hienCo.size());

        for (int i = 0; i < hopLe.size(); i++) {
            VariantForm row = hopLe.get(i);
            ProductVariant variant = i < soDongGiuLai ? hienCo.get(i) : new ProductVariant();
            variant.setProduct(product);
            variant.setName(row.getName().trim());
            variant.setExtraPrice(row.getExtraPrice() == null ? BigDecimal.ZERO : row.getExtraPrice());
            variant.setStock(Math.max(row.getStock(), 0));
            variantRepository.save(variant);
        }

        if (hienCo.size() > soDongGiuLai) {
            List<ProductVariant> thua = hienCo.subList(soDongGiuLai, hienCo.size());
            for (ProductVariant variant : thua) {
                if (cartItemRepository.existsByVariant(variant)) {
                    throw new IllegalStateException("Size \"" + variant.getName()
                            + "\" dang co trong gio hang cua khach nen khong xoa duoc. "
                            + "Hay dat ton kho = 0 de ngung ban size nay.");
                }
            }
            variantRepository.deleteAll(thua);
        }
    }
}