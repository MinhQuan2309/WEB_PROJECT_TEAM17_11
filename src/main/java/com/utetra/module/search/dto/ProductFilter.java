package com.utetra.module.search.dto;

import java.math.BigDecimal;

/**
 * Bo dieu kien tim kiem / loc san pham. Field nao null thi bo qua dieu kien do.
 *
 * sort nhan mot trong: newest, best-selling, rating, liked, price-asc, price-desc
 */
public record ProductFilter(
        String keyword,
        Long categoryId,
        Long shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort) {

    public static ProductFilter empty() {
        return new ProductFilter(null, null, null, null, null, "newest");
    }
}
