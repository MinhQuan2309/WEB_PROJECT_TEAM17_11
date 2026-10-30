package com.utetra.module.promotion;

import com.utetra.entity.DiscountType;
import com.utetra.entity.Product;
import com.utetra.entity.Promotion;
import com.utetra.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * Tinh gia san pham sau khi tru khuyen mai dang chay.
 *
 * Diem giao dung chung: trang danh sach va trang chi tiet goi de hien gia gach
 * ngang, con gio hang va dat hang goi de tinh tien that. Nho dung chung mot lop
 * nen gia hien tren man hinh luon bang gia khach phai tra.
 *
 * Ba muc khuyen mai cung co the ap cho mot san pham:
 *   - khuyen mai rieng cua san pham  (promotion.product = san pham)
 *   - khuyen mai cua cua hang        (promotion.shop = shop, product = null)
 *   - khuyen mai cua san             (shop = null, product = null)
 *
 * Khi nhieu chuong trinh cung chay, lay chuong trinh giam duoc nhieu nhat,
 * khong cong don, de khach khong bao gio tra it hon 0 dong.
 */
@Service
public class PromotionPricingService {

    private final PromotionRepository promotionRepository;

    public PromotionPricingService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /** Chuong trinh giam duoc nhieu nhat cho san pham nay, neu co. */
    @Transactional(readOnly = true)
    public Optional<Promotion> bestPromotionFor(Product product) {
        if (product == null || product.getBasePrice() == null) {
            return Optional.empty();
        }
        return promotionRepository.findRunning(LocalDateTime.now(), product.getShop()).stream()
                .filter(km -> apDungCho(km, product))
                .max(Comparator.comparing(km -> tinhGiam(km, product.getBasePrice())));
    }

    /** So tien duoc giam tren mot don vi san pham. Khong co khuyen mai thi tra ve 0. */
    @Transactional(readOnly = true)
    public BigDecimal discountFor(Product product) {
        if (product == null || product.getBasePrice() == null) {
            return BigDecimal.ZERO;
        }
        return bestPromotionFor(product)
                .map(km -> tinhGiam(km, product.getBasePrice()))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Gia goc sau khi tru khuyen mai. Day la con so dung de tinh tien o gio hang
     * va don hang, cung la con so hien ra man hinh.
     */
    @Transactional(readOnly = true)
    public BigDecimal effectiveBasePrice(Product product) {
        if (product == null || product.getBasePrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getBasePrice().subtract(discountFor(product)).max(BigDecimal.ZERO);
    }

    /** Co dang giam gia khong, dung cho giao dien quyet dinh co gach ngang gia cu hay khong. */
    @Transactional(readOnly = true)
    public boolean hasDiscount(Product product) {
        return discountFor(product).signum() > 0;
    }

    /**
     * Mot chuong trinh co ap cho san pham nay khong.
     *
     * Truy van findRunning da loc theo shop (shop cua san pham hoac khuyen mai san),
     * o day chi can chan them truong hop chuong trinh danh rieng cho san pham khac.
     */
    private boolean apDungCho(Promotion km, Product product) {
        if (km.getProduct() == null) {
            return true;
        }
        return km.getProduct().getId().equals(product.getId());
    }

    /** So tien giam cua mot chuong trinh tren mot don vi san pham. */
    private BigDecimal tinhGiam(Promotion km, BigDecimal giaGoc) {
        if (km.getValue() == null || km.getDiscountType() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal giam = switch (km.getDiscountType()) {
            case PERCENT -> giaGoc.multiply(km.getValue())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            case AMOUNT -> km.getValue();
            // Mien phi van chuyen khong lam thay doi gia mon, da duoc xu ly o phan voucher.
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
        if (km.getDiscountType() == DiscountType.PERCENT
                && km.getMaxDiscount() != null && km.getMaxDiscount().signum() > 0) {
            giam = giam.min(km.getMaxDiscount());
        }
        return giam.max(BigDecimal.ZERO).min(giaGoc);
    }
}
