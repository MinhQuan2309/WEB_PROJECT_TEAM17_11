package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

/**
 * Danh gia + binh luan san pham da mua.
 * De tai yeu cau binh luan toi thieu 50 ky tu va cho kem anh/video.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    /** Don hang chung minh la da mua san pham nay. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private int rating = 5;

    @Column(nullable = false, length = 2000)
    private String content;

    private String mediaUrl;

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
