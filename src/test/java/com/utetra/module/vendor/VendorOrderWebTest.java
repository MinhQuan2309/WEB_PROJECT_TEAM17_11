package com.utetra.module.vendor;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderRepository;
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

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiem tra lop HTTP cua tuan 3: trang danh sach / chi tiet don render duoc,
 * doi trang thai tu form chay dung, va IDOR bi chan.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendorOrderWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Order taoDon(Shop shop, OrderStatus status) {
        Order order = new Order();
        order.setCode("WEB-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setReceiverName("Khach Web");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Web, Thu Duc");
        order.setSubtotal(BigDecimal.valueOf(30000));
        order.setTotal(BigDecimal.valueOf(30000));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return orderRepository.save(order);
    }

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow()).orElseThrow();
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Trang danh sach don va chi tiet don render duoc")
    void trangDonHangRenderDuoc() throws Exception {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW);

        mockMvc.perform(get("/vendor/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/orders"));

        mockMvc.perform(get("/vendor/orders").param("status", "NEW"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vendor/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/order-detail"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Loc theo trang thai khong hop le thi quay ve danh sach kem thong bao")
    void locTrangThaiKhongHopLe() throws Exception {
        mockMvc.perform(get("/vendor/orders").param("status", "KHONG_CO_THAT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/orders"))
                .andExpect(flash().attributeExists("formError"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Doi trang thai tu form: NEW -> CONFIRMED")
    void doiTrangThaiTuForm() throws Exception {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW);

        mockMvc.perform(post("/vendor/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "CONFIRMED")
                        .param("note", "Xac nhan tu web"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/orders/" + order.getId()));

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, sau.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(sau.getNote().contains("Xac nhan tu web"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("Doi trang thai sai luat tu form thi bi chan, don giu nguyen")
    void doiTrangThaiSaiLuatBiChan() throws Exception {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW);

        mockMvc.perform(post("/vendor/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("formError"));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.NEW,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "vendor2", roles = "VENDOR")
    @DisplayName("IDOR qua HTTP: Vendor khac khong xem / doi trang thai don cua minh")
    void vendorKhacKhongXemDuocDon() throws Exception {
        Order order = taoDon(shopOf("vendor1"), OrderStatus.NEW);

        mockMvc.perform(get("/vendor/orders/" + order.getId())).andExpect(status().isForbidden());
        mockMvc.perform(post("/vendor/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.NEW,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }
}