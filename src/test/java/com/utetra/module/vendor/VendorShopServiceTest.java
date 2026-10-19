package com.utetra.module.vendor;

import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.module.vendor.dto.ShopRegistrationRequest;
import com.utetra.module.vendor.dto.ShopUpdateRequest;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu shop cua tuan 1: dang ky shop, sua thong tin shop,
 * va quan trong nhat la chan IDOR (Vendor khac khong duoc cham vao shop khong phai cua minh).
 */
@SpringBootTest
@Transactional
class VendorShopServiceTest {

    @Autowired VendorShopService vendorShopService;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private ShopRegistrationRequest dangKyForm(String name) {
        ShopRegistrationRequest form = new ShopRegistrationRequest();
        form.setName(name);
        form.setAddress("12 Vo Van Ngan, Thu Duc, TP.HCM");
        form.setDescription("Chi nhanh moi cua UTETra");
        form.setPhone("0901234567");
        return form;
    }

    private ShopUpdateRequest suaForm(String name) {
        ShopUpdateRequest form = new ShopUpdateRequest();
        form.setName(name);
        form.setAddress("99 Le Van Viet, Thu Duc, TP.HCM");
        form.setDescription("Mo ta sau khi sua");
        form.setPhone("0912345678");
        return form;
    }

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow(() -> new AssertionError("Tai khoan " + username + " chua co shop mau"));
    }

    @Test
    @DisplayName("Dang ky shop: tao shop PENDING, chiet khau mac dinh, vai cua chu shop VAN LA USER")
    void dangKyShopTaoShopChoDuyetVaIvanLaUser() {
        Shop shop = vendorShopService.registerShop("khach", dangKyForm("  UTETra Thu Duc  "), null);

        assertNotNull(shop.getId(), "Shop phai duoc luu");
        assertEquals(ShopStatus.PENDING, shop.getStatus(), "Shop moi phai o trang thai cho duyet");
        assertEquals("UTETra Thu Duc", shop.getName(), "Ten shop phai duoc cat khoang trang");
        assertEquals("khach", shop.getOwner().getUsername());
        assertEquals(0, shop.getCommissionRate().compareTo(new BigDecimal("10.00")));

        Role vaiSauKhiDangKy = userRepository.findByUsername("khach").orElseThrow().getRole();
        assertEquals(Role.USER, vaiSauKhiDangKy,
                "Chua duoc Admin/Manager duyet thi vai van la USER, khong tu nang len VENDOR");
    }

    @Test
    @DisplayName("Mot tai khoan chi duoc dang ky mot shop")
    void khongDangKyShopHaiLan() {
        assertThrows(IllegalStateException.class,
                () -> vendorShopService.registerShop("vendor1", dangKyForm("Shop thu hai"), null));
    }

    @Test
    @DisplayName("Admin/Manager/Shipper khong duoc dung form dang ky shop")
    void vaiKhacKhongDangKyDuocShop() {
        assertThrows(AccessDeniedException.class,
                () -> vendorShopService.registerShop("admin", dangKyForm("Shop cua admin"), null));
        assertThrows(AccessDeniedException.class,
                () -> vendorShopService.registerShop("shipper", dangKyForm("Shop cua shipper"), null));
    }

    @Test
    @DisplayName("IDOR: Vendor B khong sua duoc shop cua Vendor A")
    void vendorKhacKhongSuaDuocShopCuaNguoiKhac() {
        Shop shopCuaVendor1 = shopOf("vendor1");
        String tenCu = shopCuaVendor1.getName();

        assertThrows(AccessDeniedException.class, () -> vendorShopService.updateShop(
                "vendor2", shopCuaVendor1.getId(), suaForm("Bi doi ten trai phep"), null, null));

        Shop sauKhiBiTanCong = shopRepository.findById(shopCuaVendor1.getId()).orElseThrow();
        assertEquals(tenCu, sauKhiBiTanCong.getName(), "Du lieu shop khong duoc doi");
    }

    @Test
    @DisplayName("IDOR: doc shop khong phai cua minh cung bi chan")
    void khongDocDuocShopCuaNguoiKhac() {
        Shop shopCuaVendor1 = shopOf("vendor1");
        assertThrows(AccessDeniedException.class,
                () -> vendorShopService.requireOwnedShop("vendor2", shopCuaVendor1.getId()));
    }

    @Test
    @DisplayName("Shop khong ton tai tra 404 chu khong lo du lieu")
    void shopKhongTonTaiTra404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vendorShopService.requireOwnedShop("vendor1", 999_999L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Vendor sua duoc thong tin nhung KHONG doi duoc trang thai va chiet khau (mass assignment)")
    void vendorKhongDoiDuocTrangThaiVaChietKhau() {
        Shop shop = shopOf("vendor1");
        ShopStatus trangThaiCu = shop.getStatus();
        BigDecimal chietKhauCu = shop.getCommissionRate();

        vendorShopService.updateShop("vendor1", shop.getId(),
                suaForm("UTETra Le Van Viet - doi ten"), null, null);

        Shop sau = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals("UTETra Le Van Viet - doi ten", sau.getName());
        assertEquals("99 Le Van Viet, Thu Duc, TP.HCM", sau.getAddress());
        assertEquals(trangThaiCu, sau.getStatus(), "Vendor khong duoc tu doi trang thai shop");
        assertEquals(0, sau.getCommissionRate().compareTo(chietKhauCu),
                "Vendor khong duoc tu doi chiet khau san");
    }

    @Test
    @DisplayName("Shop bi tam dung thi khong sua duoc thong tin")
    void shopBiTamDungThiKhongSuaDuoc() {
        Shop shop = shopOf("vendor2");
        shop.setStatus(ShopStatus.SUSPENDED);
        shopRepository.save(shop);

        assertThrows(AccessDeniedException.class, () -> vendorShopService.updateShop(
                "vendor2", shop.getId(), suaForm("Van muon sua"), null, null));
    }

    @Test
    @DisplayName("Shop cua chinh minh thi doc va sua binh thuong")
    void chuShopThaoTacDuocTrenShopCuaMinh() {
        Shop shop = shopOf("vendor1");
        assertTrue(vendorShopService.requireOwnedShop("vendor1", shop.getId()).getId() > 0);
        assertEquals(shop.getId(), vendorShopService.requireMyShop("vendor1").getId());
    }
}