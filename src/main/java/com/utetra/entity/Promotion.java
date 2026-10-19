package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chuong trinh khuyen mai. Khac Voucher o cho khach khong phai nhap ma:
 * trong thoi gian hieu luc thi tu dong ap cho san pham / shop tuong ung.
 *
 *   shop = null  : khuyen mai cua san, do Admin hoac Manager tao
 *   shop != null : khuyen mai rieng cua shop, do Vendor tao
 *   product != null : chi ap cho dung mot san pham
 */
@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    /** Dat ten cot la discount_value vi "value" la tu khoa SQL. */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value = BigDecimal.ZERO;

    /** Tran giam toi da khi giam theo phan tram. 0 = khong gioi han. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal maxDiscount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime startAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime endAt = LocalDateTime.now().plusDays(7);

    @Column(nullable = false)
    private boolean active = true;

    /** Dang trong thoi gian chay hay khong. */
    public boolean isRunning() {
        LocalDateTime now = LocalDateTime.now();
        return active && now.isAfter(startAt) && now.isBefore(endAt);
    }

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
