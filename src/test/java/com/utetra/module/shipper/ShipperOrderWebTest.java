package com.utetra.module.shipper;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.PaymentMethod;
import com.utetra.entity.PaymentStatus;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ShipmentRepository;
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
 * Kiem tra lop HTTP cua module shipper (tuan 5): render cac trang, nhan don,
 * cap nhat trang thai, bat buoc ly do khi giao that bai, chan truy cap sai vai.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShipperOrderWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Order taoDon(OrderStatus status, String shipperUsername) {
        Shop shop = shopRepository.findByOwner(userRepository.findByUsername("vendor1").orElseThrow())
                .orElseThrow();
        Order order = new Order();
        order.setCode("SW-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setShipper(shipperUsername == null ? null
                : userRepository.findByUsername(shipperUsername).orElseThrow());
        order.setReceiverName("Khach Web");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Web, Thu Duc");
        order.setSubtotal(BigDecimal.valueOf(80000));
        order.setShippingFee(BigDecimal.valueOf(15000));
        order.setTotal(BigDecimal.valueOf(95000));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return orderRepository.save(order);
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Cac trang cua shipper render duoc")
    void cacTrangRenderDuoc() throws Exception {
        Order dangGiao = taoDon(OrderStatus.SHIPPING, "shipper");
        taoDon(OrderStatus.CONFIRMED, null);

        mockMvc.perform(get("/shipper"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/dashboard"));

        mockMvc.perform(get("/shipper/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/orders"));

        mockMvc.perform(get("/shipper/orders").param("status", "SHIPPING"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/shipper/orders/" + dangGiao.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/order-detail"));

        mockMvc.perform(get("/shipper/available"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/available"));

        mockMvc.perform(get("/shipper/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/stats"));

        mockMvc.perform(get("/shipper/stats").param("groupBy", "MONTH")
                        .param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Loc trang thai sai thi quay ve danh sach kem thong bao")
    void locTrangThaiSai() throws Exception {
        mockMvc.perform(get("/shipper/orders").param("status", "KHONG_CO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/orders"))
                .andExpect(flash().attributeExists("formError"));
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Nhan don tu trang don cho nhan: chuyen sang trang chi tiet don cua minh")
    void nhanDonTuTrangChoNhan() throws Exception {
        Order order = taoDon(OrderStatus.CONFIRMED, null);

        mockMvc.perform(post("/shipper/available/" + order.getId() + "/claim").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/orders/" + order.getId()));

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("shipper", sau.getShipper().getUsername());
        org.junit.jupiter.api.Assertions.assertTrue(shipmentRepository.findByOrder(sau).isPresent());
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Nhan don da co nguoi thi quay ve kem thong bao loi")
    void nhanDonDaCoNguoiThiBaoLoi() throws Exception {
        Order order = taoDon(OrderStatus.CONFIRMED, "vendor1");

        mockMvc.perform(post("/shipper/available/" + order.getId() + "/claim").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/available"))
                .andExpect(flash().attributeExists("formError"));
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Cap nhat trang thai tu form: dang giao -> da giao")
    void capNhatTrangThaiTuForm() throws Exception {
        Order order = taoDon(OrderStatus.SHIPPING, "shipper");

        mockMvc.perform(post("/shipper/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "DELIVERED")
                        .param("returnTo", "list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/orders"));

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.DELIVERED, sau.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PAID, sau.getPaymentStatus());
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Bao giao that bai ma khong ghi ly do thi bi chan")
    void giaoThatBaiThieuLyDoBiChan() throws Exception {
        Order order = taoDon(OrderStatus.SHIPPING, "shipper");

        mockMvc.perform(post("/shipper/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "RETURNED_REFUNDED")
                        .param("failedReason", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("formError"));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.SHIPPING,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "shipper", roles = "SHIPPER")
    @DisplayName("Giao that bai co ly do thi luu vao ban ghi giao hang")
    void giaoThatBaiCoLyDo() throws Exception {
        Order order = taoDon(OrderStatus.SHIPPING, "shipper");

        mockMvc.perform(post("/shipper/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "RETURNED_REFUNDED")
                        .param("failedReason", "Sai dia chi giao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        Order sau = orderRepository.findById(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.RETURNED_REFUNDED, sau.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Sai dia chi giao",
                shipmentRepository.findByOrder(sau).orElseThrow().getFailedReason());
    }

    @Test
    @WithMockUser(username = "shipper2", roles = "SHIPPER")
    @DisplayName("IDOR: shipper khac khong xem duoc don khong phai cua minh")
    void shipperKhacKhongXemDuocDon() throws Exception {
        Order order = taoDon(OrderStatus.SHIPPING, "shipper");

        mockMvc.perform(get("/shipper/orders/" + order.getId())).andExpect(status().isForbidden());
        mockMvc.perform(post("/shipper/orders/" + order.getId() + "/status")
                        .with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.SHIPPING,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Khach thuong khong vao duoc khu vuc shipper")
    void khachThuongBiChan() throws Exception {
        mockMvc.perform(get("/shipper")).andExpect(status().isForbidden());
        mockMvc.perform(get("/shipper/orders")).andExpect(status().isForbidden());
        mockMvc.perform(get("/shipper/stats")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chua dang nhap thi bi day ve trang login")
    void chuaDangNhapThiVeLogin() throws Exception {
        mockMvc.perform(get("/shipper/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}