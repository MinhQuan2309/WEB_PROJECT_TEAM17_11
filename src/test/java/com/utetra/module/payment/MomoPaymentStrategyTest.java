package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Kiem tra cong MOMO (tuan 4).
 * Phan goi HTTP duoc gia lap bang MockRestServiceServer nen khong can mang that.
 */
class MomoPaymentStrategyTest {

    private PaymentGatewayProperties properties;
    private MockRestServiceServer server;
    private MomoPaymentStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        strategy = new MomoPaymentStrategy(properties, builder.build());
    }

    private void cauHinh() {
        properties.getMomo().setPartnerCode("MOMO_PARTNER");
        properties.getMomo().setAccessKey("MOMO_ACCESS");
        properties.getMomo().setSecretKey("MOMO_SECRET");
        properties.getMomo().setReturnUrl("http://localhost:8080/payment/momo/return");
        properties.getMomo().setIpnUrl("http://localhost:8080/payment/momo/ipn");
    }

    private Order donHang(String code, String total) {
        Order order = new Order();
        order.setCode(code);
        order.setTotal(new BigDecimal(total));
        order.setPaymentMethod(PaymentMethod.MOMO);
        return order;
    }

    @Test
    @DisplayName("Chua cau hinh khoa thi bao loi ro rang")
    void chuaCauHinhThiBaoLoi() {
        assertFalse(strategy.isConfigured());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> strategy.initiate(donHang("DH1", "100000"), "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Chua cau hinh MOMO"));
    }

    @Test
    @DisplayName("Chuoi ky luc tao giao dich gom dung 10 tham so, sap xep theo ten")
    void chuoiKyLucTaoGiaoDich() {
        cauHinh();
        String raw = "accessKey=MOMO_ACCESS&amount=99000&extraData=&ipnUrl=http://localhost:8080/payment/momo/ipn"
                + "&orderId=DH007&orderInfo=Thanh toan don hang DH007&partnerCode=MOMO_PARTNER"
                + "&redirectUrl=http://localhost:8080/payment/momo/return&requestId=REQ1&requestType=captureWallet";

        assertEquals(PaymentSignatures.hmacSha256("MOMO_SECRET", raw),
                strategy.signCreateRequest("DH007", "REQ1", 99000L,
                        "Thanh toan don hang DH007", ""));
    }

    @Test
    @DisplayName("Tao giao dich: lay duoc payUrl tu MOMO va tra ve dung ma don")
    void taoGiaoDichLayPayUrl() {
        cauHinh();
        server.expect(requestTo(properties.getMomo().getEndpoint()))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"signature\"")))
                .andRespond(withSuccess("{\"resultCode\":0,\"payUrl\":\"https://test-payment.momo.vn/pay/abc\"}",
                        MediaType.APPLICATION_JSON));

        PaymentInitResult result = strategy.initiate(donHang("DH008", "150000"), "127.0.0.1");

        assertTrue(result.requiresRedirect());
        assertEquals("https://test-payment.momo.vn/pay/abc", result.redirectUrl());
        assertEquals("DH008", result.transactionRef());
        server.verify();
    }

    @Test
    @DisplayName("MOMO tra ve loi thi bao ro ma loi, khong chuyen huong lung tung")
    void momoBaoLoiThiBaoRoMa() {
        cauHinh();
        server.expect(requestTo(properties.getMomo().getEndpoint()))
                .andRespond(withSuccess("{\"resultCode\":1006,\"message\":\"Bad request\"}",
                        MediaType.APPLICATION_JSON));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> strategy.initiate(donHang("DH009", "150000"), "127.0.0.1"));
        assertTrue(ex.getMessage().contains("1006"));
    }

    @Test
    @DisplayName("Chu ky callback dung thi hop le, sua du lieu thi sai")
    void kiemTraChuKyCallback() {
        cauHinh();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", "MOMO_PARTNER");
        params.put("orderId", "DH010");
        params.put("requestId", "REQ10");
        params.put("amount", "150000");
        params.put("orderInfo", "Thanh toan don hang DH010");
        params.put("orderType", "momo_wallet");
        params.put("transId", "2400000001");
        params.put("resultCode", "0");
        params.put("message", "Successful.");
        params.put("payType", "qr");
        params.put("responseTime", "1699000000000");
        params.put("extraData", "");
        params.put("signature", strategy.signCallback(params));

        assertTrue(strategy.verifySignature(params));

        Map<String, String> suaDoi = new LinkedHashMap<>(params);
        suaDoi.put("amount", "1000");
        assertFalse(strategy.verifySignature(suaDoi));

        Map<String, String> themThamSoLa = new LinkedHashMap<>(params);
        themThamSoLa.put("localMessage", "Thanh cong");
        assertTrue(strategy.verifySignature(themThamSoLa),
                "MOMO tra them tham so khac thi chu ky van phai dung");

        Map<String, String> khongChuKy = new LinkedHashMap<>(params);
        khongChuKy.remove("signature");
        assertFalse(strategy.verifySignature(khongChuKy));
    }

    @Test
    @DisplayName("Doc ket qua: resultCode 0 la thanh cong")
    void docKetQua() {
        Map<String, String> ok = new LinkedHashMap<>();
        ok.put("resultCode", "0");
        ok.put("orderId", "DH011");
        ok.put("transId", "2400000002");
        ok.put("amount", "150000");

        PaymentCallbackResult thanhCong = strategy.parseCallback(ok);
        assertTrue(thanhCong.success());
        assertEquals("DH011", thanhCong.transactionRef());
        assertEquals("2400000002", thanhCong.gatewayTransactionId());
        assertEquals(0, new BigDecimal("150000").compareTo(thanhCong.amount()));

        Map<String, String> thatBai = new LinkedHashMap<>(ok);
        thatBai.put("resultCode", "1006");
        thatBai.put("message", "Bad request");
        assertFalse(strategy.parseCallback(thatBai).success());
    }

    @Test
    @DisplayName("Tham so null khong lam no ung dung")
    void thamSoNull() {
        cauHinh();
        assertFalse(strategy.verifySignature(null));
        assertFalse(strategy.verifySignature(Map.of()));
        assertNull(strategy.parseCallback(Map.of()).amount());
        assertNotNull(strategy);
    }
}