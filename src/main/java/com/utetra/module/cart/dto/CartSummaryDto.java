package com.utetra.module.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tong hop toan bo gio hang cua User.
 */
public record CartSummaryDto(
        List<CartShopGroupDto> shopGroups,
        int totalLines,
        int totalQuantity,
        BigDecimal grandSubtotal
) {
    public boolean isEmpty() {
        return shopGroups == null || shopGroups.isEmpty();
    }
}
