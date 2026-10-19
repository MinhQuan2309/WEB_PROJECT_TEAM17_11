package com.utetra;

import com.utetra.entity.Role;
import com.utetra.module.search.ProductSearchService;
import com.utetra.module.search.dto.ProductFilter;
import com.utetra.repository.CarrierRepository;
import com.utetra.repository.VoucherRepository;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra bo khung: context len duoc, repository hop le, du lieu mau tao dung,
 * mat khau da ma hoa BCrypt, tim kiem san pham chay.
 */
@SpringBootTest
class UtetraApplicationTests {

    @Autowired UserRepository userRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CarrierRepository carrierRepository;
    @Autowired VoucherRepository voucherRepository;
    @Autowired ProductSearchService productSearchService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void duLieuMauDuocTao() {
        assertEquals(6, userRepository.count(), "Phai co 6 tai khoan mau");
        assertEquals(2, shopRepository.count(), "Phai co 2 shop mau");
        assertEquals(4, categoryRepository.count(), "Phai co 4 danh muc mau");
        assertEquals(12, productRepository.count(), "Phai co 12 san pham mau");
        assertEquals(3, carrierRepository.count(), "Phai co 3 nha van chuyen mau");
    }

    @Test
    void duDu5Vai() {
        for (Role role : Role.values()) {
            assertFalse(userRepository.findByRole(role).isEmpty(),
                    "Thieu tai khoan mau cho vai " + role);
        }
    }

    @Test
    void matKhauDuocMaHoaBcrypt() {
        var admin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(admin.getPassword().startsWith("$2"), "Mat khau phai la chuoi BCrypt");
        assertTrue(passwordEncoder.matches("123456", admin.getPassword()));
    }

    @Test
    void timKiemVaLocChay() {
        var all = productSearchService.search(ProductFilter.empty(), 0, 5);
        assertEquals(12, all.getTotalElements());
        assertEquals(5, all.getContent().size());

        var theoTuKhoa = productSearchService.search(
                new ProductFilter("tran chau", null, null, null, null, "best-selling"), 0, 10);
        assertFalse(theoTuKhoa.isEmpty(), "Phai tim ra san pham co chu tran chau");
    }

    /** Bang vouchers tung khong tao duoc vi cot "value" la tu khoa SQL. */
    @Test
    void bangVoucherTruyVanDuoc() {
        assertEquals(0, voucherRepository.findByShopIsNullAndActiveTrue().size());
    }

    @Test
    void topBanChayKhongVuot10() {
        assertTrue(productRepository.findTop10ByActiveTrueOrderBySoldCountDesc().size() <= 10);
    }
}
