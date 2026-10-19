package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Size cua san pham (S / M / L). extraPrice la phu thu cong vao gia co so.
 * stock la ton kho rieng cua size do tai shop.
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private int stock = 0;

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
