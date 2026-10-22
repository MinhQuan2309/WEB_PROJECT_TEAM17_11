package com.utetra.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.module.auth.dto.LoginApiRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cac trang auth phai mo cho khach chua dang nhap,
 * va API dang nhap phai tra JWT dung.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @WithAnonymousUser
    void khachChuaDangNhapVaoDuocCacTrangAuth() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/verify-otp")).andExpect(status().isOk());
        mockMvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mockMvc.perform(get("/reset-password")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void dangKyThieuTruongThiHienLaiFormKemLoi() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "ab")
                        .param("fullName", "")
                        .param("email", "khong-phai-email")
                        .param("password", "123")
                        .param("confirmPassword", "456"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void apiDangNhapDungThiTraVeJwt() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginApiRequest("khach", "123456"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("khach"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithAnonymousUser
    void apiDangNhapSaiMatKhauThiTra401() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginApiRequest("khach", "sai-mat-khau"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void apiDangNhapThieuThongTinThiTra400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
