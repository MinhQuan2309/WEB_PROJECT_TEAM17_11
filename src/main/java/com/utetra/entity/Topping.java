package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Topping thuoc ve shop, khach chon kem khi mua (tran chau, thach, pudding...).
 */
@Entity
@Table(name = "toppings")
public class Topping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
