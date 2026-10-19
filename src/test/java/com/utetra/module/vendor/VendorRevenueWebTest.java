package com.utetra.module.vendor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiem tra lop HTTP cua bao cao doanh thu (tuan 5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendorRevenueWebTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Trang bao cao doanh thu render duoc, ca kieu gop theo ngay va theo thang")
    void trangBaoCaoRenderDuoc() throws Exception {
        mockMvc.perform(get("/vendor/revenue"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/revenue"))
                .andExpect(content().string(containsString("Doanh thu shop")));

        mockMvc.perform(get("/vendor/revenue")
                        .param("groupBy", "MONTH")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thực nhận")));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Kieu gop nhom sai thi lay mac dinh theo ngay, khong bao loi")
    void kieuGopSaiThiLayMacDinh() throws Exception {
        mockMvc.perform(get("/vendor/revenue").param("groupBy", "TUNG_GIO"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/revenue"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Tai CSV: tra file dinh kem dung dinh dang")
    void taiCsvDuoc() throws Exception {
        mockMvc.perform(get("/vendor/revenue/export.csv")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Thuc nhan")));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Khach thuong khong xem duoc bao cao doanh thu cua shop")
    void khachThuongBi403() throws Exception {
        mockMvc.perform(get("/vendor/revenue")).andExpect(status().isForbidden());
        mockMvc.perform(get("/vendor/revenue/export.csv")).andExpect(status().isForbidden());
    }

    @DisplayName("Dashboard Vendor co link sang bao cao doanh thu")
    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    void dashboardCoLinkBaoCao() throws Exception {
        mockMvc.perform(get("/vendor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/vendor/revenue")));
    }
}