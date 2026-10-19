package com.utetra.module.vendor;

import com.utetra.entity.Shop;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem tra lop HTTP cua tuan 1: form dang ky shop nam ngoai /vendor/** nen phai tu
 * kiem tra dang nhap, va /vendor/shop/{id} phai chan IDOR.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendorSecurityWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    @Test
    @DisplayName("Khach chua dang nhap vao /shop/register thi bi day ve trang dang nhap")
    void chuaDangNhapThiBiDayVeLogin() throws Exception {
        mockMvc.perform(get("/shop/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Khach da dang nhap xem duoc form dang ky shop")
    void daDangNhapXemDuocForm() throws Exception {
        mockMvc.perform(get("/shop/register")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Chu shop vao duoc trang sua shop cua minh")
    void chuShopVaoDuocTrangSuaShop() throws Exception {
        mockMvc.perform(get("/vendor/shop/" + shopOf("vendor1").getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "vendor2", roles = "VENDOR")
    @DisplayName("IDOR: Vendor khac vao trang sua shop khong phai cua minh thi bi 403")
    void vendorKhacVaoTrangSuaShopBi403() throws Exception {
        mockMvc.perform(get("/vendor/shop/" + shopOf("vendor1").getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor2", roles = "VENDOR")
    @DisplayName("IDOR: Vendor khac POST sua shop khong phai cua minh thi bi 403")
    void vendorKhacPostSuaShopBi403() throws Exception {
        mockMvc.perform(post("/vendor/shop/" + shopOf("vendor1").getId())
                        .with(csrf())
                        .param("name", "Ten bi chiem")
                        .param("address", "Dia chi bi chiem"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Khach dang nhap gui form dang ky shop -> tao shop cho duyet")
    void khachGuiFormDangKyShop() throws Exception {
        mockMvc.perform(post("/shop/register")
                        .with(csrf())
                        .param("name", "UTETra Binh Thanh")
                        .param("address", "1 Dien Bien Phu, Binh Thanh")
                        .param("phone", "0901234567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/register"));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Form dang ky thieu ten shop thi tra ve form kem loi, khong tao shop")
    void formThieuTenShopThiBaoLoi() throws Exception {
        mockMvc.perform(post("/shop/register")
                        .with(csrf())
                        .param("name", "")
                        .param("address", "1 Dien Bien Phu, Binh Thanh"))
                .andExpect(status().isOk());
    }
}