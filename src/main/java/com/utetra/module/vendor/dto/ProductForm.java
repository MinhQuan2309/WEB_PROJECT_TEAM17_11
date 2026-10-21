package com.utetra.module.vendor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Form them / sua san pham cua shop (tuan 2).
 *
 * Khong co truong shop: shop duoc lay tu tai khoan dang dang nhap o tang service,
 * nen Vendor khong the gan san pham sang shop cua nguoi khac qua form.
 * Cac truong thong ke (soldCount, ratingAvg, likeCount, viewCount) cung khong co
 * vi do la du lieu he thong tu tinh.
 */
public class ProductForm {

    @NotBlank(message = "Ten san pham khong duoc de trong")
    @Size(max = 200, message = "Ten san pham toi da 200 ky tu")
    private String name;

    @Size(max = 2000, message = "Mo ta toi da 2000 ky tu")
    private String description;

    @NotNull(message = "Gia co so khong duoc de trong")
    @DecimalMin(value = "0", message = "Gia co so khong duoc am")
    @Digits(integer = 10, fraction = 2, message = "Gia co so khong hop le")
    private BigDecimal basePrice = BigDecimal.ZERO;

    /** Danh muc co the de trong; neu dien thi phai la danh muc dang hoat dong. */
    private Long categoryId;

    private boolean active = true;

    /** Cac size cua san pham (Size S / M / L...). Dong nao bo trong ten thi bi bo qua. */
    @Valid
    private List<VariantForm> variants = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<VariantForm> getVariants() { return variants; }
    public void setVariants(List<VariantForm> variants) { this.variants = variants; }
}