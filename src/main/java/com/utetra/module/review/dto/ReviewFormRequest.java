package com.utetra.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Du lieu form danh gia (1-5 sao) va binh luan (>= 50 ky tu) cho san pham da mua.
 */
public class ReviewFormRequest {

    private Long productId;
    private Long orderId;

    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    private int rating = 5;

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(min = 50, max = 2000, message = "Nội dung bình luận phải có tối thiểu 50 ký tự")
    private String content;

    private String mediaUrl;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
