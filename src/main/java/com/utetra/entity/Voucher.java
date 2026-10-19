package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ma giam gia. shop = null nghia la voucher cua san (Admin/Manager phat hanh),
 * shop != null la voucher rieng cua shop do (Vendor phat hanh).
 */
@Entity
@Table(name = "vouchers")
public class Voucher extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    /** Dat ten cot la discount_value vi "value" la tu khoa SQL. */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(nullable = false)
    private int usedCount = 0;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMinOrderTotal() { return minOrderTotal; }
    public void setMinOrderTotal(BigDecimal minOrderTotal) { this.minOrderTotal = minOrderTotal; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
