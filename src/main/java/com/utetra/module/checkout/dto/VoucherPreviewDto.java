package com.utetra.module.checkout.dto;

import java.math.BigDecimal;

public record VoucherPreviewDto(
        boolean valid,
        String code,
        String message,
        String discountTypeLabel,
        BigDecimal discountAmount,
        BigDecimal finalTotal
) {
}
