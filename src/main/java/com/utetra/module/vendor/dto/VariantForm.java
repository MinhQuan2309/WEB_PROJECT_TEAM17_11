package com.utetra.module.vendor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Mot dong size trong form san pham (tuan 2).
 * id = null nghia la size moi; id co san nghia la dang sua size cu.
 */
public class VariantForm {

    private Long id;

    @Size(max = 50, message = "Ten size toi da 50 ky tu")
    private String name;

    @DecimalMin(value = "0", message = "Phu thu size khong duoc am")
    @Digits(integer = 10, fraction = 2, message = "Phu thu size khong hop le")
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Min(value = 0, message = "Ton kho khong duoc am")
    private int stock = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}