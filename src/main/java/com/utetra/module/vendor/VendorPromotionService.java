package com.utetra.module.vendor;

import com.utetra.entity.DiscountType;
import com.utetra.entity.Product;
import com.utetra.entity.Promotion;
import com.utetra.entity.Shop;
import com.utetra.entity.Voucher;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.PromotionRepository;
import com.utetra.repository.VoucherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Khuyen mai cua Vendor, theo yeu cau de tai muc Vendor: "tao chuong trinh khuyen mai".
 *
 * Shop co hai thu khac nhau:
 *
 *   1. Chuong trinh khuyen mai (Promotion) - khach khong phai nhap gi,
 *      trong thoi gian hieu luc thi hien ngay tren trang chi tiet san pham.
 *   2. Ma giam gia (Voucher) - khach nhap ma o buoc thanh toan.
 *      CheckoutService da biet lay voucher cua shop nen tao xong la dung duoc.
 *
 * Moi thao tac deu di qua shop cua chinh nguoi dang dang nhap, nen Vendor A
 * khong sua duoc khuyen mai cua Vendor B du co doan dung id tren URL.
 */
@Service
public class VendorPromotionService {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    private final VendorShopService vendorShopService;
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final ProductRepository productRepository;

    public VendorPromotionService(VendorShopService vendorShopService,
                                  PromotionRepository promotionRepository,
                                  VoucherRepository voucherRepository,
                                  ProductRepository productRepository) {
        this.vendorShopService = vendorShopService;
        this.promotionRepository = promotionRepository;
        this.voucherRepository = voucherRepository;
        this.productRepository = productRepository;
    }

    // ------------------------------------------------------ chuong trinh

    @Transactional(readOnly = true)
    public List<Promotion> promotionsOf(String username) {
        return promotionRepository.findByShopOrderByIdDesc(vendorShopService.requireMyShop(username));
    }

    @Transactional
    public Promotion savePromotion(String username, Long id, String name, String description,
                                   DiscountType type, BigDecimal value, BigDecimal maxDiscount,
                                   Long productId, LocalDateTime startAt, LocalDateTime endAt) {

        Shop shop = vendorShopService.requireMyShop(username);
        validateDiscount(name, type, value, startAt, endAt);

        Promotion promotion = (id == null) ? new Promotion() : requireMyPromotion(shop, id);
        promotion.setShop(shop);
        promotion.setProduct(resolveProductOfShop(shop, productId));
        promotion.setName(name.trim());
        promotion.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        promotion.setDiscountType(type);
        promotion.setValue(value);
        promotion.setMaxDiscount(maxDiscount == null ? BigDecimal.ZERO : maxDiscount);
        promotion.setStartAt(startAt);
        promotion.setEndAt(endAt);
        return promotionRepository.save(promotion);
    }

    @Transactional
    public Promotion togglePromotion(String username, Long id) {
        Promotion promotion = requireMyPromotion(vendorShopService.requireMyShop(username), id);
        promotion.setActive(!promotion.isActive());
        return promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(String username, Long id) {
        promotionRepository.delete(requireMyPromotion(vendorShopService.requireMyShop(username), id));
    }

    // ---------------------------------------------------------- ma giam gia

    @Transactional(readOnly = true)
    public List<Voucher> vouchersOf(String username) {
        return voucherRepository.findByShopOrderByIdDesc(vendorShopService.requireMyShop(username));
    }

    @Transactional
    public Voucher saveVoucher(String username, Long id, String code, DiscountType type,
                               BigDecimal value, BigDecimal minOrderTotal, Integer quantity,
                               LocalDateTime startAt, LocalDateTime endAt) {

        Shop shop = vendorShopService.requireMyShop(username);
        validateDiscount(code, type, value, startAt, endAt);

        String normalized = code.trim().toUpperCase();
        Voucher voucher = (id == null) ? new Voucher() : requireMyVoucher(shop, id);

        boolean codeChanged = voucher.getCode() == null || !voucher.getCode().equals(normalized);
        if (codeChanged && voucherRepository.existsByCodeIgnoreCase(normalized)) {
            throw new IllegalStateException("Ma giam gia " + normalized + " da ton tai, hay dat ma khac");
        }

        voucher.setShop(shop);
        voucher.setCode(normalized);
        voucher.setDiscountType(type);
        voucher.setValue(value);
        voucher.setMinOrderTotal(minOrderTotal == null ? BigDecimal.ZERO : minOrderTotal);
        voucher.setQuantity(quantity == null ? 0 : Math.max(quantity, 0));
        voucher.setStartAt(startAt);
        voucher.setEndAt(endAt);
        return voucherRepository.save(voucher);
    }

    @Transactional
    public Voucher toggleVoucher(String username, Long id) {
        Voucher voucher = requireMyVoucher(vendorShopService.requireMyShop(username), id);
        voucher.setActive(!voucher.isActive());
        return voucherRepository.save(voucher);
    }

    @Transactional
    public void deleteVoucher(String username, Long id) {
        Voucher voucher = requireMyVoucher(vendorShopService.requireMyShop(username), id);
        if (voucher.getUsedCount() > 0) {
            throw new IllegalStateException(
                    "Ma nay da co " + voucher.getUsedCount() + " luot dung, chi tat di chu khong xoa duoc");
        }
        voucherRepository.delete(voucher);
    }

    /** San pham cua shop, de Vendor chon khi chi muon giam cho mot mon. */
    @Transactional(readOnly = true)
    public List<Product> productsOfMyShop(String username) {
        Shop shop = vendorShopService.requireMyShop(username);
        return productRepository.findByShopOrderByIdDesc(shop,
                org.springframework.data.domain.PageRequest.of(0, 200)).getContent();
    }

    // ---------------------------------------------------------------- noi bo

    private void validateDiscount(String name, DiscountType type, BigDecimal value,
                                  LocalDateTime startAt, LocalDateTime endAt) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Ten chuong trinh hoac ma giam gia khong duoc de trong");
        }
        if (type == null) {
            throw new IllegalStateException("Chua chon kieu giam gia");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Muc giam khong duoc am");
        }
        if (type == DiscountType.PERCENT && value.compareTo(MAX_PERCENT) > 0) {
            throw new IllegalStateException("Giam theo phan tram khong duoc vuot qua 100");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalStateException("Phai chon thoi gian bat dau va ket thuc");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalStateException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
    }

    private Product resolveProductOfShop(Shop shop, Long productId) {
        if (productId == null) {
            return null;
        }
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new IllegalStateException("Khong tim thay san pham #" + productId));
        if (product.getShop() == null || !product.getShop().getId().equals(shop.getId())) {
            throw new AccessDeniedException("San pham nay khong thuoc shop cua ban");
        }
        return product;
    }

    private Promotion requireMyPromotion(Shop shop, Long id) {
        Promotion promotion = promotionRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay chuong trinh #" + id));
        if (promotion.getShop() == null || !promotion.getShop().getId().equals(shop.getId())) {
            throw new AccessDeniedException("Chuong trinh nay khong thuoc shop cua ban");
        }
        return promotion;
    }

    private Voucher requireMyVoucher(Shop shop, Long id) {
        Voucher voucher = voucherRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay ma giam gia #" + id));
        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shop.getId())) {
            throw new AccessDeniedException("Ma giam gia nay khong thuoc shop cua ban");
        }
        return voucher;
    }
}
