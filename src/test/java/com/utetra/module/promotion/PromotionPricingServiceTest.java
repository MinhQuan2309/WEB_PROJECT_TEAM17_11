package com.utetra.module.promotion;

import com.utetra.entity.DiscountType;
import com.utetra.entity.Product;
import com.utetra.entity.Promotion;
import com.utetra.entity.Shop;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.PromotionRepository;
import com.utetra.repository.ShopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Khuyen mai phai tru thang vao gia san pham, va gia hien ra man hinh
 * phai bang gia dung de tinh tien.
 */
@SpringBootTest
@Transactional
class PromotionPricingServiceTest {

    @Autowired PromotionPricingService pricingService;
    @Autowired PromotionRepository promotionRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;

    private Product sanPhamDauTien() {
        return productRepository.findAll().get(0);
    }

    private Promotion taoKhuyenMai(Shop shop, Product product, DiscountType loai,
                                   BigDecimal giaTri, BigDecimal giamToiDa) {
        Promotion km = new Promotion();
        km.setShop(shop);
        km.setProduct(product);
        km.setName("Test khuyen mai");
        km.setDiscountType(loai);
        km.setValue(giaTri);
        km.setMaxDiscount(giamToiDa);
        km.setStartAt(LocalDateTime.now().minusHours(1));
        km.setEndAt(LocalDateTime.now().plusDays(1));
        km.setActive(true);
        return promotionRepository.save(km);
    }

    @Test
    @DisplayName("Khong co khuyen mai thi gia giu nguyen")
    void khongCoKhuyenMaiThiGiuNguyenGia() {
        Product p = sanPhamDauTien();

        assertFalse(pricingService.hasDiscount(p));
        assertEquals(0, pricingService.effectiveBasePrice(p).compareTo(p.getBasePrice()));
    }

    @Test
    @DisplayName("Khuyen mai cua san ap cho moi san pham")
    void khuyenMaiCuaSanApChoMoiSanPham() {
        Product p = sanPhamDauTien();
        BigDecimal giaGoc = p.getBasePrice();
        taoKhuyenMai(null, null, DiscountType.PERCENT, new BigDecimal("10"), BigDecimal.ZERO);

        BigDecimal giamMongDoi = giaGoc.multiply(new BigDecimal("10"))
                .divide(new BigDecimal("100"), 0, java.math.RoundingMode.HALF_UP);

        assertTrue(pricingService.hasDiscount(p));
        assertEquals(0, pricingService.discountFor(p).compareTo(giamMongDoi));
        assertEquals(0, pricingService.effectiveBasePrice(p)
                .compareTo(giaGoc.subtract(giamMongDoi)));
    }

    @Test
    @DisplayName("Nhieu chuong trinh cung chay thi lay cai giam nhieu nhat, khong cong don")
    void layChuongTrinhGiamNhieuNhat() {
        Product p = sanPhamDauTien();
        BigDecimal giaGoc = p.getBasePrice();

        taoKhuyenMai(null, null, DiscountType.PERCENT, new BigDecimal("10"), BigDecimal.ZERO);
        taoKhuyenMai(p.getShop(), null, DiscountType.AMOUNT, new BigDecimal("12000"), BigDecimal.ZERO);

        BigDecimal giamPhanTram = giaGoc.multiply(new BigDecimal("10"))
                .divide(new BigDecimal("100"), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal giamLonNhat = giamPhanTram.max(new BigDecimal("12000"));

        assertEquals(0, pricingService.discountFor(p).compareTo(giamLonNhat),
                "Phai lay chuong trinh giam nhieu nhat chu khong cong don");
    }

    @Test
    @DisplayName("Muc giam toi da chan bot khuyen mai phan tram")
    void giamToiDaChanBotKhuyenMaiPhanTram() {
        Product p = sanPhamDauTien();
        taoKhuyenMai(null, null, DiscountType.PERCENT, new BigDecimal("50"), new BigDecimal("5000"));

        assertEquals(0, pricingService.discountFor(p).compareTo(new BigDecimal("5000")));
    }

    @Test
    @DisplayName("Gia sau khuyen mai khong bao gio am")
    void giaSauKhuyenMaiKhongAm() {
        Product p = sanPhamDauTien();
        taoKhuyenMai(null, null, DiscountType.AMOUNT, new BigDecimal("999999"), BigDecimal.ZERO);

        assertTrue(pricingService.effectiveBasePrice(p).signum() >= 0);
    }

    @Test
    @DisplayName("Khuyen mai het han thi khong ap")
    void khuyenMaiHetHanThiKhongAp() {
        Product p = sanPhamDauTien();
        Promotion km = taoKhuyenMai(null, null, DiscountType.PERCENT, new BigDecimal("20"), BigDecimal.ZERO);
        km.setStartAt(LocalDateTime.now().minusDays(10));
        km.setEndAt(LocalDateTime.now().minusDays(1));
        promotionRepository.save(km);

        assertFalse(pricingService.hasDiscount(p));
    }
}
