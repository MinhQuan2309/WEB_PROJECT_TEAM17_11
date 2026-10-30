package com.utetra.module.manager;

import com.utetra.entity.Product;
import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.entity.User;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pham vi cua Manager: chi thay va chi sua duoc du lieu cua shop minh phu trach.
 */
@SpringBootTest
@Transactional
class ManagerScopeServiceTest {

    @Autowired ManagerScopeService scopeService;
    @Autowired ShopRepository shopRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        manager = userRepository.findByUsername("manager").orElseThrow();
        admin = userRepository.findByUsername("admin").orElseThrow();
    }

    private User newUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setFullName("Test " + username);
        u.setEmail(username + "@utetra.test");
        u.setRole(role);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private Shop newShop(String name, User owner, User shopManager) {
        Shop s = new Shop();
        s.setOwner(owner);
        s.setManager(shopManager);
        s.setName(name);
        s.setAddress("1 Vo Van Ngan");
        s.setStatus(ShopStatus.ACTIVE);
        s.setCommissionRate(new BigDecimal("10.00"));
        return shopRepository.save(s);
    }

    private Product newProduct(Shop shop, String name) {
        Product p = new Product();
        p.setShop(shop);
        p.setName(name);
        p.setBasePrice(new BigDecimal("30000"));
        return productRepository.save(p);
    }

    @Test
    void managerChiThayShopMinhPhuTrach() {
        User managerKhac = newUser("manager_khac_scope", Role.MANAGER);
        Shop shopNguoiKhac = newShop("Shop cua nguoi khac", newUser("chu_khac_scope", Role.USER), managerKhac);

        var shops = scopeService.shopsOf(manager);

        assertTrue(shops.stream().noneMatch(s -> s.getId().equals(shopNguoiKhac.getId())),
                "Khong duoc thay shop cua Manager khac");
        assertTrue(shops.stream().allMatch(s -> s.getManager() != null
                        && s.getManager().getId().equals(manager.getId())));
    }

    @Test
    void adminThayTatCaShop() {
        long tongSoShop = shopRepository.count();

        assertEquals(tongSoShop, scopeService.shopsOf(admin).size());
    }

    @Test
    void managerChiThaySanPhamCuaShopMinhPhuTrach() {
        User managerKhac = newUser("manager_khac_sp", Role.MANAGER);
        Shop shopNguoiKhac = newShop("Shop ngoai pham vi", newUser("chu_ngoai_sp", Role.USER), managerKhac);
        Product spNgoai = newProduct(shopNguoiKhac, "San pham ngoai pham vi");

        var page = scopeService.productsOf(manager, 0);

        assertTrue(page.getContent().stream().noneMatch(p -> p.getId().equals(spNgoai.getId())),
                "Khong duoc thay san pham cua shop ngoai pham vi");
    }

    @Test
    void managerAnDuocSanPhamTrongPhamVi() {
        Shop shopCuaMinh = scopeService.shopsOf(manager).get(0);
        Product p = newProduct(shopCuaMinh, "San pham trong pham vi");
        assertTrue(p.isActive());

        Product sau = scopeService.toggleProductActive(manager, p.getId());

        assertFalse(sau.isActive(), "An roi thi khach khong con thay");
    }

    @Test
    void managerKhongAnDuocSanPhamNgoaiPhamVi() {
        User managerKhac = newUser("manager_khac_an", Role.MANAGER);
        Shop shopNguoiKhac = newShop("Shop khong duoc dung", newUser("chu_khong_dung", Role.USER), managerKhac);
        Product spNgoai = newProduct(shopNguoiKhac, "San pham cam dung");

        var ex = assertThrows(AccessDeniedException.class,
                () -> scopeService.toggleProductActive(manager, spNgoai.getId()));
        assertTrue(ex.getMessage().contains("pham vi"));
        assertTrue(productRepository.findById(spNgoai.getId()).orElseThrow().isActive(),
                "San pham ngoai pham vi phai giu nguyen");
    }

    @Test
    void adminAnDuocMoiSanPham() {
        User managerKhac = newUser("manager_bat_ky", Role.MANAGER);
        Shop shopBatKy = newShop("Shop bat ky", newUser("chu_bat_ky", Role.USER), managerKhac);
        Product p = newProduct(shopBatKy, "San pham bat ky");

        Product sau = scopeService.toggleProductActive(admin, p.getId());

        assertFalse(sau.isActive());
    }

    @Test
    void chuShopTrongPhamViKhongTrungLap() {
        var owners = scopeService.shopOwnersOf(manager);

        assertEquals(owners.size(), owners.stream().map(User::getId).distinct().count(),
                "Mot chu shop chi hien mot lan");
    }

    @Test
    void soLieuDashboardChiTinhTrongPhamVi() {
        var summary = scopeService.summaryOf(manager);
        var shops = scopeService.shopsOf(manager);

        assertEquals(shops.size(), summary.shopCount());
        assertTrue(summary.productCount() >= 0);
        assertTrue(summary.revenue().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void managerChuaDuocPhanCongThiSoLieuBangKhong() {
        User managerMoi = newUser("manager_chua_co_shop", Role.MANAGER);

        var summary = scopeService.summaryOf(managerMoi);

        assertEquals(0, summary.shopCount());
        assertEquals(0, summary.productCount());
        assertTrue(scopeService.productsOf(managerMoi, 0).isEmpty());
    }

    // ============================================== phan cong shipper

    @Test
    void managerDatDuocNguoiDungThuongLamShipper() {
        User nguoiThuong = newUser("ung_vien_shipper", Role.USER);

        User sauKhiDat = scopeService.toggleShipperRole(manager, nguoiThuong.getId());

        assertEquals(Role.SHIPPER, sauKhiDat.getRole());
    }

    @Test
    void managerGoDuocVaiShipperKhiKhongCondonDangGiao() {
        User shipper = newUser("shipper_roi_ranh", Role.SHIPPER);

        User sauKhiGo = scopeService.toggleShipperRole(manager, shipper.getId());

        assertEquals(Role.USER, sauKhiGo.getRole());
    }

    @Test
    void khongDungToiVaiVendorHoacManager() {
        User chuShop = newUser("chu_shop_khong_doi", Role.VENDOR);
        User managerKhac = newUser("manager_khong_doi", Role.MANAGER);

        assertThrows(IllegalStateException.class,
                () -> scopeService.toggleShipperRole(manager, chuShop.getId()));
        assertThrows(IllegalStateException.class,
                () -> scopeService.toggleShipperRole(manager, managerKhac.getId()));
    }

    @Test
    void khongTuDoiVaiTroCuaChinhMinh() {
        assertThrows(IllegalStateException.class,
                () -> scopeService.toggleShipperRole(manager, manager.getId()));
    }

    @Test
    void nguoiDungThuongKhongDuocPhanCongShipper() {
        User khach = newUser("khach_thuong_khong_duoc", Role.USER);
        User ungVien = newUser("ung_vien_khac", Role.USER);

        assertThrows(AccessDeniedException.class,
                () -> scopeService.toggleShipperRole(khach, ungVien.getId()));
    }

    @Test
    void danhSachUngVienChiGomNguoiDungThuongVaShipper() {
        newUser("ung_vien_danh_sach", Role.USER);

        var ungVien = scopeService.shipperCandidates();

        assertFalse(ungVien.isEmpty());
        assertTrue(ungVien.stream().allMatch(u -> u.getRole() == Role.USER
                        || u.getRole() == Role.SHIPPER),
                "Danh sach khong duoc chua ADMIN, MANAGER hay VENDOR");
    }
}
