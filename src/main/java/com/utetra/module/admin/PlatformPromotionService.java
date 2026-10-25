package com.utetra.module.admin;

import com.utetra.entity.DiscountType;
import com.utetra.entity.Promotion;
import com.utetra.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chuong trinh khuyen mai cua san (shop = null), do Admin tao.
 * Co hai kieu theo de tai: giam phan tram tren san pham va mien/giam phi van chuyen.
 */
@Service
public class PlatformPromotionService {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    private final PromotionRepository promotionRepository;

    public PlatformPromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return promotionRepository.findByShopIsNullOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Promotion> findRunning() {
        return promotionRepository.findRunning(LocalDateTime.now(), null);
    }

    @Transactional
    public Promotion save(Long id, String name, String description, DiscountType type,
                          BigDecimal value, BigDecimal maxDiscount,
                          LocalDateTime startAt, LocalDateTime endAt) {

        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Ten chuong trinh khong duoc de trong");
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

        Promotion promotion = (id == null) ? new Promotion() : require(id);
        promotion.setShop(null);
        promotion.setProduct(null);
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
    public Promotion toggleActive(Long id) {
        Promotion promotion = require(id);
        promotion.setActive(!promotion.isActive());
        return promotionRepository.save(promotion);
    }

    @Transactional
    public void delete(Long id) {
        promotionRepository.delete(require(id));
    }

    private Promotion require(Long id) {
        return promotionRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay chuong trinh #" + id));
    }
}
