package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Mot dong trong gio hang: san pham + size + topping + muc duong/da.
 */
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cart_item_toppings",
            joinColumns = @JoinColumn(name = "cart_item_id"),
            inverseJoinColumns = @JoinColumn(name = "topping_id"))
    private List<Topping> toppings = new ArrayList<>();

    @Column(nullable = false)
    private int quantity = 1;

    /** 0 - 100 phan tram. */
    @Column(nullable = false)
    private int sugarLevel = 100;

    @Column(nullable = false)
    private int iceLevel = 100;

    @Column(length = 255)
    private String note;

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public List<Topping> getToppings() { return toppings; }
    public void setToppings(List<Topping> toppings) { this.toppings = toppings; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getSugarLevel() { return sugarLevel; }
    public void setSugarLevel(int sugarLevel) { this.sugarLevel = sugarLevel; }
    public int getIceLevel() { return iceLevel; }
    public void setIceLevel(int iceLevel) { this.iceLevel = iceLevel; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
