package com.utetra.module.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chan cua khu kiem duyet: ai vao duoc /admin/shops va /manager/shops.
 * Tai khoan dung trong test la tai khoan mau do DataSeeder tao.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShopModerationWebTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void chuaDangNhapThiBiDayVeTrangDangNhap() throws Exception {
        mockMvc.perform(get("/admin/shops"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    void khachKhongVaoDuocKhuKiemDuyet() throws Exception {
        mockMvc.perform(get("/admin/shops")).andExpect(status().isForbidden());
        mockMvc.perform(get("/manager/shops")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    void vendorKhongVaoDuocKhuKiemDuyet() throws Exception {
        mockMvc.perform(get("/admin/shops")).andExpect(status().isForbidden());
        mockMvc.perform(get("/manager/shops")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminVaoDuocCaHaiKhu() throws Exception {
        mockMvc.perform(get("/admin/shops")).andExpect(status().isOk());
        mockMvc.perform(get("/manager/shops")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerVaoDuocKhuCuaManagerNhungKhongVaoKhuAdmin() throws Exception {
        mockMvc.perform(get("/manager/shops")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/shops")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    void khachKhongGoiDuocLenhDuyet() throws Exception {
        mockMvc.perform(post("/admin/shops/1/approve").with(
                        org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}
