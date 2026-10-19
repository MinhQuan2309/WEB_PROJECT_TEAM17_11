package com.utetra.module.search;

import com.utetra.entity.Product;
import com.utetra.module.search.dto.ProductFilter;
import com.utetra.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Tim kiem va loc san pham dung chung cho toan he thong:
 * trang chu, trang danh muc, trang shop, man quan tri.
 *
 *   Page<Product> page = productSearchService.search(filter, 0, 12);
 */
@Service
public class ProductSearchService {

    public static final int DEFAULT_PAGE_SIZE = 12;

    private final ProductRepository productRepository;

    public ProductSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> search(ProductFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                size > 0 ? size : DEFAULT_PAGE_SIZE,
                sortOf(filter.sort()));
        return productRepository.findAll(ProductSpecifications.from(filter), pageable);
    }

    public Page<Product> search(ProductFilter filter, int page) {
        return search(filter, page, DEFAULT_PAGE_SIZE);
    }

    /** Doi tu khoa sap xep tren URL thanh Sort cua Spring Data. */
    public static Sort sortOf(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "best-selling" -> Sort.by(Sort.Direction.DESC, "soldCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAvg", "ratingCount");
            case "liked" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
