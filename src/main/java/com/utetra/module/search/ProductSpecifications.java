package com.utetra.module.search;

import com.utetra.entity.Product;
import com.utetra.module.search.dto.ProductFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Ghep dieu kien WHERE dong cho tim kiem san pham.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> from(ProductFilter filter) {
        Specification<Product> spec = activeOnly();
        if (StringUtils.hasText(filter.keyword())) {
            spec = spec.and(keyword(filter.keyword()));
        }
        if (filter.categoryId() != null) {
            spec = spec.and(category(filter.categoryId()));
        }
        if (filter.shopId() != null) {
            spec = spec.and(shop(filter.shopId()));
        }
        if (filter.minPrice() != null || filter.maxPrice() != null) {
            spec = spec.and(priceBetween(filter));
        }
        return spec;
    }

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> keyword(String keyword) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Product> category(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> shop(Long shopId) {
        return (root, query, cb) -> cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<Product> priceBetween(ProductFilter filter) {
        return (root, query, cb) -> {
            if (filter.minPrice() != null && filter.maxPrice() != null) {
                return cb.between(root.get("basePrice"), filter.minPrice(), filter.maxPrice());
            }
            if (filter.minPrice() != null) {
                return cb.greaterThanOrEqualTo(root.get("basePrice"), filter.minPrice());
            }
            return cb.lessThanOrEqualTo(root.get("basePrice"), filter.maxPrice());
        };
    }
}
