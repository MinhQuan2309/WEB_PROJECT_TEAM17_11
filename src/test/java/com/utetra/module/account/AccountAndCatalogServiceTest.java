package com.utetra.module.account;

import com.utetra.entity.Address;
import com.utetra.entity.Product;
import com.utetra.entity.User;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.account.dto.PasswordChangeRequest;
import com.utetra.module.account.dto.ProfileUpdateRequest;
import com.utetra.module.catalog.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiem thu nghiep vu Tuan 1 (Catalog) va Tuan 2 (Chi tiet SP, Profile, Doi mat khau, So dia chi) cua Hai.
 */
@SpringBootTest
@Transactional
class AccountAndCatalogServiceTest {

    @Autowired CatalogService catalogService;
    @Autowired AccountService accountService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Tuan 1: Trang chu Guest hien thi tren 10 san pham ban chay va User co 4 block 20 san pham")
    void trangChuGuestVaUserHoatDongDung() {
        List<Product> guestProducts = catalogService.getGuestBestSellers();
        assertTrue(guestProducts.size() > 10, "Guest phai thay tren 10 san pham");

        assertEquals(12, catalogService.getNewestProducts(0, 20).getTotalElements());
        assertEquals(12, catalogService.getBestSellingProducts(0, 20).getTotalElements());
        assertEquals(12, catalogService.getTopRatedProducts(0, 20).getTotalElements());
        assertEquals(12, catalogService.getMostLikedProducts(0, 20).getTotalElements());
    }

    @Test
    @DisplayName("Tuan 2: Cap nhat thong tin ca nhan va doi mat khau BCrypt")
    void capNhatProfileVaDoiMatKhauBcrypt() {
        User khach = accountService.getRequiredUser("khach");

        ProfileUpdateRequest profileReq = new ProfileUpdateRequest();
        profileReq.setFullName("Hoang Van Khach Moi");
        profileReq.setEmail("khach@utetra.vn");
        profileReq.setPhone("0988777666");
        accountService.updateProfile(khach, profileReq, null);

        User afterProfile = accountService.getRequiredUser("khach");
        assertEquals("Hoang Van Khach Moi", afterProfile.getFullName());
        assertEquals("0988777666", afterProfile.getPhone());

        PasswordChangeRequest pwReq = new PasswordChangeRequest();
        pwReq.setCurrentPassword("123456");
        pwReq.setNewPassword("654321");
        pwReq.setConfirmPassword("654321");
        accountService.changePassword(afterProfile, pwReq);

        assertTrue(passwordEncoder.matches("654321", accountService.getRequiredUser("khach").getPassword()));
    }

    @Test
    @DisplayName("Tuan 2: Quan ly nhieu dia chi nhan hang (CRUD va dat dia chi mac dinh)")
    void quanLyNhieuDiaChiNhanHang() {
        User khach = accountService.getRequiredUser("khach");

        AddressFormRequest addr1 = new AddressFormRequest();
        addr1.setReceiverName("Khach Dia Chi 1");
        addr1.setPhone("0901111111");
        addr1.setStreet("01 Vo Van Ngan");
        addr1.setWard("Linh Chieu");
        addr1.setDistrict("Thu Duc");
        addr1.setProvince("TP.HCM");
        addr1.setDefaultAddress(true);
        Address saved1 = accountService.saveAddress(khach, addr1);
        assertTrue(saved1.isDefaultAddress());

        AddressFormRequest addr2 = new AddressFormRequest();
        addr2.setReceiverName("Khach Dia Chi 2");
        addr2.setPhone("0902222222");
        addr2.setStreet("207 Le Van Viet");
        addr2.setWard("Hiep Phu");
        addr2.setDistrict("Thu Duc");
        addr2.setProvince("TP.HCM");
        addr2.setDefaultAddress(true);
        Address saved2 = accountService.saveAddress(khach, addr2);

        assertTrue(saved2.isDefaultAddress(), "Dia chi 2 dat mac dinh thi phai la true");
        List<Address> all = accountService.getAddresses(khach);
        assertEquals(2, all.size());
        long defaultCount = all.stream().filter(Address::isDefaultAddress).count();
        assertEquals(1, defaultCount, "Chi duoc co duy nhat 1 dia chi mac dinh");
    }
}
