package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Gio hang luu tren CSDL (de tai yeu cau ro), moi user mot gio.
 */
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}
