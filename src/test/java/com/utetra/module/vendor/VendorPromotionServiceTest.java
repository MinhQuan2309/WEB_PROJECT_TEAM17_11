package com.utetra.module.vendor;

import com.utetra.entity.DiscountType;
import com.utetra.entity.Product;
import com.utetra.entity.Promotion;
import com.utetra.entity.Shop;
import com.utetra.entity.Voucher;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.PromotionRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import com.utetra.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Khuyen mai cua Vendor: chuong trinh khuyen mai va ma giam gia cua shop.
 *
 * Diem quan trong nhat: Vendor chi dung duoc vao khuyen mai cua shop minh.
 */
@SpringBootTest
@Transactional
class VendorPromotionServiceTest {

    private static final String VENDOR1 = "vendor1";
    private static final String VENDOR2 = "vendor2";

    @Autowired VendorPromotionService promotionService;
    @Autowired VendorShopService vendorShopService;
    @Autowired PromotionRepository promotionRepository;
    @Autowired VoucherRepository voucherRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private LocalDateTime tuGio() {
        return LocalDateTime.now().minusMinutes(5);
    }

    private LocalDateTime denMai() {
        return LocalDateTime.now().plusDays(1);
    }

    private Promotion taoChuongTrinh(String username, String ten) {
        return promotionService.savePromotion(username, null, ten, "mo ta",
                DiscountType.PERCENT, new BigDecimal("15"), new BigDecimal("20000"),
                null, tuGio(), denMai());
    }

    private Voucher taoMa(String username, String code) {
        return promotionService.saveVoucher(username, null, code, DiscountType.AMOUNT,
                new BigDecimal("10000"), new BigDecimal("50000"), 100, tuGio(), denMai());
    }

    // ---------------------------------------------------- chuong trinh

    @Test
    void vendorTaoDuocChuongTrinhChoShopCuaMinh() {
        Shop shopCuaToi = vendorShopService.requireMyShop(VENDOR1);

        Promotion p = taoChuongTrinh(VENDOR1, "Giam 15 phan tram cuoi tuan");

        assertEquals(shopCuaToi.getId(), p.getShop().getId(), "Phai gan dung shop cua nguoi tao");
        assertTrue(p.isRunning(), "Dang trong thoi gian thi phai chay");
        assertNull(p.getProduct(), "Khong chon san pham thi ap toan shop");
    }

    @Test
    void chuongTrinhChiApChoMotSanPhamCuaShop() {
        Shop shop = vendorShopService.requireMyShop(VENDOR1);
        Product sanPham = productRepository.findByShopOrderByIdDesc(shop,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);

        Promotion p = promotionService.savePromotion(VENDOR1, null, "Giam rieng mot mon", null,
                DiscountType.PERCENT, new BigDecimal("20"), null,
                sanPham.getId(), tuGio(), denMai());

        assertEquals(sanPham.getId(), p.getProduct().getId());
    }

    @Test
    void khongApChuongTrinhChoSanPhamCuaShopKhac() {
        Shop shopKhac = shopRepository.findByOwner(userRepository.findByUsername(VENDOR2).orElseThrow())
                .orElseThrow();
        Product sanPhamShopKhac = productRepository.findByShopOrderByIdDesc(shopKhac,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);

        assertThrows(AccessDeniedException.class,
                () -> promotionService.savePromotion(VENDOR1, null, "Giam lan san", null,
                        DiscountType.PERCENT, new BigDecimal("10"), null,
                        sanPhamShopKhac.getId(), tuGio(), denMai()));
    }

    @Test
    void vendorKhongSuaDuocChuongTrinhCuaShopKhac() {
        Promotion cuaVendor2 = taoChuongTrinh(VENDOR2, "Chuong trinh cua shop 2");

        assertThrows(AccessDeniedException.class,
                () -> promotionService.togglePromotion(VENDOR1, cuaVendor2.getId()));
        assertThrows(AccessDeniedException.class,
                () -> promotionService.deletePromotion(VENDOR1, cuaVendor2.getId()));
    }

    @Test
    void vendorChiThayChuongTrinhCuaShopMinh() {
        taoChuongTrinh(VENDOR1, "Cua shop 1");
        Promotion cuaShop2 = taoChuongTrinh(VENDOR2, "Cua shop 2");

        var danhSach = promotionService.promotionsOf(VENDOR1);

        assertTrue(danhSach.stream().noneMatch(p -> p.getId().equals(cuaShop2.getId())));
        assertTrue(danhSach.stream().allMatch(p -> p.getShop().getOwner().getUsername().equals(VENDOR1)));
    }

    @Test
    void khongChoGiamQua100PhanTram() {
        var ex = assertThrows(IllegalStateException.class,
                () -> promotionService.savePromotion(VENDOR1, null, "Giam qua tay", null,
                        DiscountType.PERCENT, new BigDecimal("150"), null, null, tuGio(), denMai()));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void thoiGianKetThucPhaiSauBatDau() {
        assertThrows(IllegalStateException.class,
                () -> promotionService.savePromotion(VENDOR1, null, "Sai thoi gian", null,
                        DiscountType.AMOUNT, new BigDecimal("5000"), null, null,
                        denMai(), tuGio()));
    }

    @Test
    void tatChuongTrinhThiKhongConChay() {
        Promotion p = taoChuongTrinh(VENDOR1, "Tat thu");

        Promotion sau = promotionService.togglePromotion(VENDOR1, p.getId());

        assertFalse(sau.isActive());
        assertFalse(sau.isRunning());
    }

    // ------------------------------------------------------- ma giam gia

    @Test
    void vendorTaoDuocMaGiamGiaChoShopCuaMinh() {
        Shop shop = vendorShopService.requireMyShop(VENDOR1);

        Voucher v = taoMa(VENDOR1, "shop1sale");

        assertEquals("SHOP1SALE", v.getCode(), "Ma luon duoc chuyen thanh chu hoa");
        assertEquals(shop.getId(), v.getShop().getId());
        assertTrue(v.isActive());
    }

    @Test
    void khongTaoTrungMaGiamGia() {
        taoMa(VENDOR1, "TRUNGMA");

        var ex = assertThrows(IllegalStateException.class, () -> taoMa(VENDOR2, "trungma"));
        assertTrue(ex.getMessage().contains("da ton tai"));
    }

    @Test
    void vendorKhongSuaDuocMaCuaShopKhac() {
        Voucher cuaVendor2 = taoMa(VENDOR2, "MASHOP2");

        assertThrows(AccessDeniedException.class,
                () -> promotionService.toggleVoucher(VENDOR1, cuaVendor2.getId()));
    }

    @Test
    void khongXoaMaDaCoNguoiDung() {
        Voucher v = taoMa(VENDOR1, "DADUNGROI");
        v.setUsedCount(3);
        voucherRepository.save(v);

        var ex = assertThrows(IllegalStateException.class,
                () -> promotionService.deleteVoucher(VENDOR1, v.getId()));
        assertTrue(ex.getMessage().contains("luot dung"));
    }

    @Test
    void xoaDuocMaChuaAiDung() {
        Voucher v = taoMa(VENDOR1, "CHUAAIDUNG");

        promotionService.deleteVoucher(VENDOR1, v.getId());

        assertTrue(voucherRepository.findById(v.getId()).isEmpty());
    }

    @Test
    void maGiamGiaCuaShopNamTrongDanhSachKhachCoTheDung() {
        Shop shop = vendorShopService.requireMyShop(VENDOR1);
        Voucher v = taoMa(VENDOR1, "KHACHDUNGDUOC");

        // Day chinh la danh sach ma CheckoutService doc khi khach thanh toan
        var dungDuoc = voucherRepository.findByShopAndActiveTrue(shop);

        assertTrue(dungDuoc.stream().anyMatch(x -> x.getId().equals(v.getId())),
                "Ma vua tao phai xuat hien o buoc thanh toan");
    }
}
