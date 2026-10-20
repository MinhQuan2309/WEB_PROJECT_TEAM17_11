package com.utetra.module.catalog.dto;

import com.utetra.entity.Product;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO phuc vu lazy loading 4 block 20 san pham tren trang chu User.
 */
public record ProductBlockItemDto(
        Long id,
        String name,
        String shopName,
        Long shopId,
        String categoryName,
        BigDecimal basePrice,
        String formattedPrice,
        String imageUrl,
        BigDecimal ratingAvg,
        int ratingCount,
        int soldCount,
        int likeCount
) {
    public static ProductBlockItemDto from(Product p) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        String priceStr = (p.getBasePrice() != null ? nf.format(p.getBasePrice()) : "0") + " đ";
        String img = (p.getImageUrl() != null && !p.getImageUrl().isBlank())
                ? p.getImageUrl()
                : "https://placehold.co/400x300?text=UTETra";
        return new ProductBlockItemDto(
                p.getId(),
                p.getName(),
                p.getShop() != null ? p.getShop().getName() : "UTETra Shop",
                p.getShop() != null ? p.getShop().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : "",
                p.getBasePrice(),
                priceStr,
                img,
                p.getRatingAvg(),
                p.getRatingCount(),
                p.getSoldCount(),
                p.getLikeCount()
        );
    }
}
