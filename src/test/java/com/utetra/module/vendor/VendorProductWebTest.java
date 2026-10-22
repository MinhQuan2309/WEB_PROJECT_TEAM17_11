package com.utetra.module.vendor;

import com.utetra.entity.Product;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiem tra lop HTTP cua tuan 2: cac trang quan ly san pham render duoc,
 * tao san pham tu form chay dung, va IDOR bi chan ngay o tang HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendorProductWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Long sanPhamCuaVendor1() {
        var shop = shopRepository.findByOwner(userRepository.findByUsername("vendor1").orElseThrow())
                .orElseThrow();
        return productRepository.findByShopOrderByIdDesc(shop, PageRequest.of(0, 1))
                .getContent().get(0).getId();
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Cac trang quan ly san pham / topping / dashboard render duoc")
    void cacTrangQuanLyRenderDuoc() throws Exception {
        mockMvc.perform(get("/vendor")).andExpect(status().isOk()).andExpect(view().name("vendor/dashboard"));
        mockMvc.perform(get("/vendor/products")).andExpect(status().isOk()).andExpect(view().name("vendor/products"));
        mockMvc.perform(get("/vendor/products/new")).andExpect(status().isOk()).andExpect(view().name("vendor/product-form"));
        mockMvc.perform(get("/vendor/products/" + sanPhamCuaVendor1() + "/edit"))
                .andExpect(status().isOk()).andExpect(view().name("vendor/product-form"));
        mockMvc.perform(get("/vendor/toppings")).andExpect(status().isOk()).andExpect(view().name("vendor/toppings"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Vendor tao san pham tu form: luu san pham va size")
    void taoSanPhamTuForm() throws Exception {
        long truoc = productRepository.count();

        mockMvc.perform(post("/vendor/products")
                        .with(csrf())
                        .param("name", "Tra sua tu form web")
                        .param("basePrice", "39000")
                        .param("description", "Mo ta tu form")
                        .param("active", "true")
                        .param("variants[0].name", "Size M")
                        .param("variants[0].extraPrice", "5000")
                        .param("variants[0].stock", "20")
                        .param("variants[1].name", "Size L")
                        .param("variants[1].extraPrice", "10000")
                        .param("variants[1].stock", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/products"));

        org.junit.jupiter.api.Assertions.assertEquals(truoc + 1, productRepository.count());
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Form thieu ten san pham thi tra lai form kem loi, khong luu")
    void thieuTenSanPhamThiBaoLoi() throws Exception {
        long truoc = productRepository.count();

        mockMvc.perform(post("/vendor/products")
                        .with(csrf())
                        .param("name", "")
                        .param("basePrice", "39000"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/product-form"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));

        org.junit.jupiter.api.Assertions.assertEquals(truoc, productRepository.count());
    }

    @Test
    @WithMockUser(username = "vendor2", roles = "VENDOR")
    @DisplayName("IDOR qua HTTP: Vendor khac khong mo duoc form sua san pham cua minh")
    void vendorKhacKhongMoDuocFormSua() throws Exception {
        mockMvc.perform(get("/vendor/products/" + sanPhamCuaVendor1() + "/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor2", roles = "VENDOR")
    @DisplayName("IDOR qua HTTP: Vendor khac khong POST sua / xoa san pham cua minh")
    void vendorKhacKhongSuaXoaDuocSanPham() throws Exception {
        Long idCuaVendor1 = sanPhamCuaVendor1();
        String tenCu = productRepository.findById(idCuaVendor1).map(Product::getName).orElseThrow();

        mockMvc.perform(post("/vendor/products/" + idCuaVendor1)
                        .with(csrf())
                        .param("name", "Ten bi chiem")
                        .param("basePrice", "1000"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/vendor/products/" + idCuaVendor1 + "/toggle").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/vendor/products/" + idCuaVendor1 + "/delete").with(csrf()))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(tenCu,
                productRepository.findById(idCuaVendor1).orElseThrow().getName());
    }

    @Test
    @DisplayName("Chua dang nhap thi khong vao duoc khu vendor")
    void chuaDangNhapKhongVaoDuocVendor() throws Exception {
        mockMvc.perform(get("/vendor/products")).andExpect(status().is3xxRedirection());
    }
}