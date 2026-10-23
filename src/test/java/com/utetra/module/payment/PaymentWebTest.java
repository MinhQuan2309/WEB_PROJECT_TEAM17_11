package com.utetra.module.payment;

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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiem tra lop HTTP cua module thanh toan (tuan 4):
 * trang thanh toan, bat dau thanh toan, IPN khong bi chan boi dang nhap, chan IDOR.
 */
@SpringBootTest(properties = {
        "app.payment.vnpay.tmn-code=TESTCODE",
        "app.payment.vnpay.hash-secret=SECRETKEYTEST",
        "app.payment.vnpay.return-url=http://localhost:8080/payment/vnpay/return",
        "app.payment.momo.partner-code=MOMO_PARTNER",
        "app.payment.momo.access-key=MOMO_ACCESS",
        "app.payment.momo.secret-key=MOMO_SECRET"
})
@AutoConfigureMockMvc
@Transactional
class PaymentWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired PaymentGatewayProperties properties;
    @Autowired OrderRepository orderRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Order taoDon(PaymentMethod method) {
        Shop shop = shopRepository.findByOwner(userRepository.findByUsername("vendor1").orElseThrow())
                .orElseThrow();
        Order order = new Order();
        order.setCode("PW-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(shop);
        order.setReceiverName("Khach Web");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Web, Thu Duc");
        order.setSubtotal(BigDecimal.valueOf(70000));
        order.setTotal(BigDecimal.valueOf(70000));
        order.setStatus(OrderStatus.NEW);
        order.setPaymentMethod(method);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return orderRepository.save(order);
    }

    private Map<String, String> callbackVnpay(String transactionRef, String soTien, String responseCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", responseCode);
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_TransactionNo", "14123456");
        params.put("vnp_Amount", soTien);
        String hashData = PaymentSignatures.buildVnpayHashData(PaymentSignatures.sortedParams(params));
        params.put("vnp_SecureHash",
                PaymentSignatures.hmacSha512(properties.getVnpay().getHashSecret(), hashData));
        return params;
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Trang thanh toan va trang ket qua render duoc")
    void trangThanhToanRenderDuoc() throws Exception {
        Order order = taoDon(PaymentMethod.COD);

        mockMvc.perform(get("/payment/checkout/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/checkout"));

        mockMvc.perform(get("/payment/result").param("ref", order.getCode()))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/result"));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Bam thanh toan COD thi quay ve trang ket qua")
    void batDauThanhToanCod() throws Exception {
        Order order = taoDon(PaymentMethod.COD);

        mockMvc.perform(post("/payment/start/" + order.getId())
                        .with(csrf())
                        .param("method", "COD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment/result?ref=" + order.getCode()));
    }

    @Test
    @WithMockUser(username = "khach", roles = "USER")
    @DisplayName("Chon VNPAY thi chuyen sang cong thanh toan sandbox")
    void batDauThanhToanVnpay() throws Exception {
        Order order = taoDon(PaymentMethod.COD);

        mockMvc.perform(post("/payment/start/" + order.getId())
                        .with(csrf())
                        .param("method", "VNPAY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://sandbox.vnpayment.vn/**"));
    }

    @Test
    @WithMockUser(username = "vendor1", roles = "VENDOR")
    @DisplayName("IDOR: nguoi khac khong xem duoc trang thanh toan cua don nguoi ta")
    void nguoiKhacKhongXemDuocTrangThanhToan() throws Exception {
        Order order = taoDon(PaymentMethod.COD);

        mockMvc.perform(get("/payment/checkout/" + order.getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/payment/result").param("ref", order.getCode()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chua dang nhap thi bi day ve trang login, khong vao duoc trang thanh toan")
    void chuaDangNhapThiVeLogin() throws Exception {
        Order order = taoDon(PaymentMethod.COD);

        mockMvc.perform(get("/payment/checkout/" + order.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("IPN cua VNPAY khong bi chan boi dang nhap, va chu ky sai thi tra RspCode 97")
    void ipnVnpayChuKySai() throws Exception {
        Order order = taoDon(PaymentMethod.VNPAY);
        Map<String, String> params = callbackVnpay(order.getCode(), "7000000", "00");
        params.put("vnp_SecureHash", "chu-ky-bia");

        var request = get("/payment/vnpay/ipn");
        params.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    @Test
    @DisplayName("IPN chu ky dung thi xac nhan thanh cong va don chuyen sang da tra tien")
    void ipnVnpayChuKyDung() throws Exception {
        Order order = taoDon(PaymentMethod.VNPAY);
        Map<String, String> params = callbackVnpay(order.getCode(), "7000000", "00");

        var request = get("/payment/vnpay/ipn");
        params.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PAID,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("Return URL cua VNPAY dua khach ve trang ket qua")
    void returnUrlVnpayQuayVeTrangKetQua() throws Exception {
        Order order = taoDon(PaymentMethod.VNPAY);
        Map<String, String> params = callbackVnpay(order.getCode(), "7000000", "00");

        var request = get("/payment/vnpay/return");
        params.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment/result?ref=" + order.getCode()));
    }

    @Test
    @DisplayName("IPN cua MOMO chu ky sai thi tra 400, khong cap nhat don")
    void ipnMomoChuKySai() throws Exception {
        Order order = taoDon(PaymentMethod.MOMO);

        mockMvc.perform(post("/payment/momo/ipn")
                        .contentType("application/json")
                        .content("{\"orderId\":\"" + order.getCode() + "\",\"resultCode\":0,"
                                + "\"amount\":70000,\"signature\":\"chu-ky-bia\"}"))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test
    @DisplayName("Trang chu khong bi anh huong boi chuoi loc rieng cua module thanh toan")
    void trangChuVanVaoDuocKhongCanDangNhap() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}