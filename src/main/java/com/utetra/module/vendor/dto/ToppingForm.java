package com.utetra.module.vendor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form them / sua topping cua shop (tuan 2).
 * Topping thuoc ve shop nao do tang service quyet dinh theo tai khoan dang nhap.
 */
public class ToppingForm {

    @NotBlank(message = "Ten topping khong duoc de trong")
    @Size(max = 100, message = "Ten topping toi da 100 ky tu")
    private String name;

    @NotNull(message = "Gia topping khong duoc de trong")
    @DecimalMin(value = "0", message = "Gia topping khong duoc am")
    @Digits(integer = 10, fraction = 2, message = "Gia topping khong hop le")
    private BigDecimal price = BigDecimal.ZERO;

    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}