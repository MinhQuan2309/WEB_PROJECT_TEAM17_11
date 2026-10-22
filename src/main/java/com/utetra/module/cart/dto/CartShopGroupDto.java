package com.utetra.module.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nhom cac CartItem theo tung Shop trong gio hang.
 */
public record CartShopGroupDto(
        Long shopId,
        String shopName,
        String shopAddress,
        List<CartItemViewDto> items,
        int totalQuantity,
        BigDecimal subtotal
) {
}
