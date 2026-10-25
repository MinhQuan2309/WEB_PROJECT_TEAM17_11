package com.utetra.module.admin;

import com.utetra.common.util.SlugUtils;
import com.utetra.entity.Carrier;
import com.utetra.entity.Category;
import com.utetra.entity.DiscountType;
import com.utetra.entity.Product;
import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Khu quan tri: nguoi dung, danh muc, nha van chuyen, khuyen mai san, chiet khau.
 */
@SpringBootTest
@Transactional
class AdminManagementServiceTest {

    @Autowired UserAdminService userAdminService;
    @Autowired CategoryAdminService categoryAdminService;
    @Autowired CarrierAdminService carrierAdminService;
    @Autowired PlatformPromotionService promotionService;
    @Autowired ShopModerationService moderationService;

    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;

    // ------------------------------------------------------- nguoi dung

    @Test
    void timKiemUserTheoTuKhoaVaVaiTro() {
        var ketQua = userAdminService.search("admin", null, 0);
        assertTrue(ketQua.getTotalElements() >= 1);

        var theoVai = userAdminService.search(null, Role.SHIPPER, 0);
        assertTrue(theoVai.getContent().stream().allMatch(u -> u.getRole() == Role.SHIPPER));
    }

    @Test
    void khoaVaMoKhoaTaiKhoan() {
        User khach = userRepository.findByUsername("khach").orElseThrow();
        assertFalse(khach.isLocked());

        userAdminService.toggleLock("admin", khach.getId());
        assertTrue(userRepository.findById(khach.getId()).orElseThrow().isLocked());

        userAdminService.toggleLock("admin", khach.getId());
        assertFalse(userRepository.findById(khach.getId()).orElseThrow().isLocked());
    }

    @Test
    void adminKhongTuKhoaChinhMinh() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        var ex = assertThrows(IllegalStateException.class,
                () -> userAdminService.toggleLock("admin", admin.getId()));
        assertTrue(ex.getMessage().contains("chinh minh"));
    }

    @Test
    void adminKhongTuDoiVaiCuaChinhMinh() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        assertThrows(IllegalStateException.class,
                () -> userAdminService.changeRole("admin", admin.getId(), Role.USER));
    }

    @Test
    void doiVaiTroTaiKhoanKhac() {
        User khach = userRepository.findByUsername("khach").orElseThrow();

        User sau = userAdminService.changeRole("admin", khach.getId(), Role.SHIPPER);

        assertEquals(Role.SHIPPER, sau.getRole());
    }

    @Test
    void khongHaVaiVendorDangSoHuuShop() {
        User vendor1 = userRepository.findByUsername("vendor1").orElseThrow();

        var ex = assertThrows(IllegalStateException.class,
                () -> userAdminService.changeRole("admin", vendor1.getId(), Role.USER));
        assertTrue(ex.getMessage().contains("shop"));
    }

    // ---------------------------------------------------------- danh muc

    @Test
    void themDanhMucThiSinhSlugKhongDau() {
        Category c = categoryAdminService.save(null, "Trà sữa trân châu", null, null);

        assertEquals("tra-sua-tran-chau", c.getSlug());
        assertTrue(c.isActive());
    }

    @Test
    void slugTrungThiThemHauTo() {
        categoryAdminService.save(null, "Cà phê đá", null, null);
        Category thuHai = categoryAdminService.save(null, "Cà phê đá", null, null);

        assertEquals("ca-phe-da-2", thuHai.getSlug());
    }

    @Test
    void khongChonDanhMucConLamCha() {
        Category cha = categoryAdminService.save(null, "Nhom cha", null, null);
        Category con = categoryAdminService.save(null, "Nhom con", cha.getId(), null);

        var ex = assertThrows(IllegalStateException.class,
                () -> categoryAdminService.save(null, "Nhom chau", con.getId(), null));
        assertTrue(ex.getMessage().contains("hai cap"));
    }

    @Test
    void khongXoaDanhMucDangCoSanPham() {
        Category traSua = categoryRepository.findBySlug("tra-sua").orElseThrow();
        assertTrue(productRepository.countByCategory(traSua) > 0);

        var ex = assertThrows(IllegalStateException.class,
                () -> categoryAdminService.delete(traSua.getId()));
        assertTrue(ex.getMessage().contains("san pham"));
    }

    @Test
    void xoaDuocDanhMucRong() {
        Category rong = categoryAdminService.save(null, "Danh muc rong", null, null);
        categoryAdminService.delete(rong.getId());

        assertTrue(categoryRepository.findById(rong.getId()).isEmpty());
    }

    @Test
    void slugUtilsDoiTiengVietSangKhongDau() {
        assertEquals("tra-sua-tran-chau-duong-den", SlugUtils.toSlug("Trà sữa trân châu đường đen"));
        assertEquals("do-an-kem", SlugUtils.toSlug("  Đồ ăn kèm  "));
    }

    // -------------------------------------------------- nha van chuyen

    @Test
    void themVaSuaNhaVanChuyen() {
        Carrier c = carrierAdminService.save(null, "Giao sieu toc", new BigDecimal("25000"));
        assertEquals("Giao sieu toc", c.getName());

        Carrier sua = carrierAdminService.save(c.getId(), "Giao sieu toc 2h", new BigDecimal("30000"));
        assertEquals("Giao sieu toc 2h", sua.getName());
        assertEquals(0, sua.getFee().compareTo(new BigDecimal("30000")));
    }

    @Test
    void khongChoPhiVanChuyenAm() {
        assertThrows(IllegalStateException.class,
                () -> carrierAdminService.save(null, "Phi am", new BigDecimal("-1000")));
    }

    @Test
    void xoaDuocNhaVanChuyenChuaDungTrongDon() {
        Carrier c = carrierAdminService.save(null, "Chua dung den", new BigDecimal("1000"));
        carrierAdminService.delete(c.getId());
    }

    // -------------------------------------------------- khuyen mai san

    @Test
    void taoKhuyenMaiSan() {
        var p = promotionService.save(null, "Giam 10 phan tram", "Ap dung toan san",
                DiscountType.PERCENT, new BigDecimal("10"), new BigDecimal("20000"),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3));

        assertTrue(p.isRunning(), "Dang trong thoi gian thi phai la dang chay");
        assertTrue(promotionService.findRunning().stream().anyMatch(x -> x.getId().equals(p.getId())));
    }

    @Test
    void khongChoGiamQua100PhanTram() {
        var ex = assertThrows(IllegalStateException.class,
                () -> promotionService.save(null, "Giam qua tay", null, DiscountType.PERCENT,
                        new BigDecimal("150"), null,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1)));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void thoiGianKetThucPhaiSauBatDau() {
        assertThrows(IllegalStateException.class,
                () -> promotionService.save(null, "Sai thoi gian", null, DiscountType.AMOUNT,
                        new BigDecimal("5000"), null,
                        LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1)));
    }

    @Test
    void tatKhuyenMaiThiKhongConChay() {
        var p = promotionService.save(null, "Tat thu", null, DiscountType.FREE_SHIPPING,
                BigDecimal.ZERO, null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));

        promotionService.toggleActive(p.getId());

        assertFalse(promotionService.findAll().stream()
                .filter(x -> x.getId().equals(p.getId()))
                .findFirst().orElseThrow().isRunning());
    }

    // ------------------------------------------------------- chiet khau

    @Test
    void adminDatChietKhauChoShop() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Shop shop = shopRepository.findAll().get(0);

        Shop sau = moderationService.setCommissionRate(admin, shop.getId(), new BigDecimal("12.5"));

        assertEquals(0, sau.getCommissionRate().compareTo(new BigDecimal("12.5")));
    }

    @Test
    void chietKhauNgoaiKhoang0Den100ThiBiTuChoi() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Shop shop = shopRepository.findAll().get(0);

        assertThrows(IllegalStateException.class,
                () -> moderationService.setCommissionRate(admin, shop.getId(), new BigDecimal("120")));
        assertThrows(IllegalStateException.class,
                () -> moderationService.setCommissionRate(admin, shop.getId(), new BigDecimal("-5")));
    }

    @Test
    void sanPhamVanTruyVanDuocSauKhiDoiDanhMuc() {
        Category moi = categoryAdminService.save(null, "Danh muc moi cho test", null, null);
        Product p = productRepository.findAll().get(0);
        p.setCategory(moi);
        productRepository.save(p);

        assertEquals(1, productRepository.countByCategory(moi));
    }
}
