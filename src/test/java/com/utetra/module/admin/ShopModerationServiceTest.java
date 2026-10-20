package com.utetra.module.admin;

import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.entity.User;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem duyet shop. Diem quan trong nhat: chi khi duoc duyet thi chu shop
 * moi duoc nang len VENDOR, va Manager khong thoc tay vao shop cua Manager khac.
 */
@SpringBootTest
@Transactional
class ShopModerationServiceTest {

    @Autowired ShopModerationService moderationService;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User admin;
    private User manager;
    private User owner;
    private Shop pendingShop;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByUsername("admin").orElseThrow();
        manager = userRepository.findByUsername("manager").orElseThrow();
        owner = newUser("chu_shop_test", Role.USER);
        pendingShop = newShop(owner, null, ShopStatus.PENDING);
    }

    @Test
    void duyetShopThiNangChuShopLenVendor() {
        assertEquals(Role.USER, owner.getRole(), "truoc khi duyet van la USER");

        Shop approved = moderationService.approve(admin, pendingShop.getId());

        assertEquals(ShopStatus.ACTIVE, approved.getStatus());
        assertEquals(Role.VENDOR, userRepository.findById(owner.getId()).orElseThrow().getRole());
    }

    @Test
    void tuChoiThiChuShopVanLaUser() {
        Shop rejected = moderationService.reject(admin, pendingShop.getId(), "Dia chi khong ro rang");

        assertEquals(ShopStatus.REJECTED, rejected.getStatus());
        assertEquals("Dia chi khong ro rang", rejected.getModerationNote());
        assertEquals(Role.USER, userRepository.findById(owner.getId()).orElseThrow().getRole(),
                "Tu choi thi khong duoc nang quyen");
    }

    @Test
    void tuChoiPhaiGhiLyDo() {
        var ex = assertThrows(IllegalStateException.class,
                () -> moderationService.reject(admin, pendingShop.getId(), "   "));
        assertTrue(ex.getMessage().contains("ly do"));
    }

    @Test
    void managerDuyetShopChuaAiNhanThiNhanLuonShopDo() {
        assertNull(pendingShop.getManager());

        Shop approved = moderationService.approve(manager, pendingShop.getId());

        assertEquals(ShopStatus.ACTIVE, approved.getStatus());
        assertSame(manager.getId(), approved.getManager().getId());
    }

    @Test
    void managerKhongDungDuocVaoShopCuaManagerKhac() {
        User managerKhac = newUser("manager_khac_test", Role.MANAGER);
        Shop shopCuaNguoiKhac = newShop(newUser("chu_shop_2_test", Role.USER),
                managerKhac, ShopStatus.PENDING);

        assertThrows(AccessDeniedException.class,
                () -> moderationService.approve(manager, shopCuaNguoiKhac.getId()));
    }

    @Test
    void khongDuyetLaiShopDangHoatDong() {
        moderationService.approve(admin, pendingShop.getId());

        var ex = assertThrows(IllegalStateException.class,
                () -> moderationService.approve(admin, pendingShop.getId()));
        assertTrue(ex.getMessage().contains("da duoc duyet"));
    }

    @Test
    void tamDungRoiMoLaiShop() {
        moderationService.approve(admin, pendingShop.getId());

        Shop suspended = moderationService.suspend(admin, pendingShop.getId(), "Vi pham quy dinh");
        assertEquals(ShopStatus.SUSPENDED, suspended.getStatus());

        Shop reactivated = moderationService.reactivate(admin, pendingShop.getId());
        assertEquals(ShopStatus.ACTIVE, reactivated.getStatus());
        assertNull(reactivated.getModerationNote(), "Mo lai thi xoa ghi chu cu");
    }

    @Test
    void moLaiHoSoBiTuChoiThiVeChoDuyet() {
        moderationService.reject(admin, pendingShop.getId(), "Thieu thong tin");

        Shop reopened = moderationService.reopen(admin, pendingShop.getId());

        assertEquals(ShopStatus.PENDING, reopened.getStatus());
        assertNull(reopened.getModerationNote());
    }

    @Test
    void chiAdminMoiPhanCongDuocNguoiQuanLy() {
        assertThrows(AccessDeniedException.class,
                () -> moderationService.assignManager(manager, pendingShop.getId(), manager.getId()));

        Shop assigned = moderationService.assignManager(admin, pendingShop.getId(), manager.getId());
        assertEquals(manager.getId(), assigned.getManager().getId());
    }

    @Test
    void khongPhanCongDuocNguoiKhongPhaiManager() {
        var ex = assertThrows(IllegalStateException.class,
                () -> moderationService.assignManager(admin, pendingShop.getId(), owner.getId()));
        assertTrue(ex.getMessage().contains("khong phai Manager"));
    }

    @Test
    void managerChiThayShopCuaMinhVaShopChuaAiNhan() {
        User managerKhac = newUser("manager_khac_2_test", Role.MANAGER);
        newShop(newUser("chu_shop_3_test", Role.USER), managerKhac, ShopStatus.PENDING);

        var thayDuoc = moderationService.visibleShops(manager, ShopStatus.PENDING);

        assertTrue(thayDuoc.stream().anyMatch(s -> s.getId().equals(pendingShop.getId())),
                "phai thay shop chua ai nhan");
        assertTrue(thayDuoc.stream().allMatch(s -> s.getManager() == null
                        || s.getManager().getId().equals(manager.getId())),
                "khong duoc thay shop cua Manager khac");
    }

    // ---------------------------------------------------------------- helper

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

    private Shop newShop(User owner, User manager, ShopStatus status) {
        Shop s = new Shop();
        s.setOwner(owner);
        s.setManager(manager);
        s.setName("Shop test cua " + owner.getUsername());
        s.setAddress("1 Vo Van Ngan, Thu Duc");
        s.setStatus(status);
        s.setCommissionRate(new BigDecimal("10.00"));
        return shopRepository.save(s);
    }
}
