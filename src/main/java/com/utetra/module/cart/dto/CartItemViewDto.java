package com.utetra.module.cart.dto;

import com.utetra.entity.CartItem;
import com.utetra.entity.Topping;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * DTO hien thi mot dong trong gio hang kem don gia (da cong phu thu size + topping) va thanh tien.
 */
public record CartItemViewDto(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        Long shopId,
        String shopName,
        String variantName,
        String toppingsText,
        int sugarLevel,
        int iceLevel,
        String note,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static CartItemViewDto from(CartItem item) {
        BigDecimal base = item.getProduct().getBasePrice() != null
                ? item.getProduct().getBasePrice()
                : BigDecimal.ZERO;
        BigDecimal variantExtra = (item.getVariant() != null && item.getVariant().getExtraPrice() != null)
                ? item.getVariant().getExtraPrice()
                : BigDecimal.ZERO;
        BigDecimal toppingExtra = item.getToppings() != null
                ? item.getToppings().stream()
                    .map(t -> t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        BigDecimal unit = base.add(variantExtra).add(toppingExtra);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(Math.max(1, item.getQuantity())));

        String toppingsStr = (item.getToppings() == null || item.getToppings().isEmpty())
                ? "Không thêm topping"
                : item.getToppings().stream().map(Topping::getName).collect(Collectors.joining(", "));

        String img = (item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isBlank())
                ? item.getProduct().getImageUrl()
                : "https://placehold.co/200x150?text=UTETra";

        return new CartItemViewDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                img,
                item.getProduct().getShop().getId(),
                item.getProduct().getShop().getName(),
                item.getVariant() != null ? item.getVariant().getName() : "Tiêu chuẩn",
                toppingsStr,
                item.getSugarLevel(),
                item.getIceLevel(),
                item.getNote(),
                item.getQuantity(),
                unit,
                total
        );
    }
}
