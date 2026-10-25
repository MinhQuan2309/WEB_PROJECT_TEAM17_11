package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra cong VNPAY (tuan 4) - khong can goi mang, chi kiem tra URL va chu ky.
 */
class VnPayPaymentStrategyTest {

    private PaymentGatewayProperties properties;
    private VnPayPaymentStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        strategy = new VnPayPaymentStrategy(properties);
    }

    private Order donHang(String code, String total) {
        Order order = new Order();
        order.setCode(code);
        order.setTotal(new BigDecimal(total));
        order.setPaymentMethod(PaymentMethod.VNPAY);
        return order;
    }

    private void cauHinh() {
        properties.getVnpay().setTmnCode("TESTCODE");
        properties.getVnpay().setHashSecret("SECRETKEYTEST");
        properties.getVnpay().setReturnUrl("http://localhost:8080/payment/vnpay/return");
    }

    /** Tach query string thanh Map, giong nhu luc cong goi ve server. */
    private Map<String, String> tachThamSo(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        for (String cap : query.split("&")) {
            int bang = cap.indexOf('=');
            if (bang > 0) {
                params.put(cap.substring(0, bang),
                        URLDecoder.decode(cap.substring(bang + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    @Test
    @DisplayName("Chua cau hinh khoa thi bao loi ro rang, khong tao giao dich")
    void chuaCauHinhThiBaoLoi() {
        assertFalse(strategy.isConfigured());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> strategy.initiate(donHang("DH1", "100000"), "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Chua cau hinh VNPAY"));
    }

    @Test
    @DisplayName("Tao URL thanh toan: dung so tien x100, co du tham so va chu ky")
    void taoUrlThanhToan() {
        cauHinh();

        PaymentInitResult result = strategy.initiate(donHang("DH001", "125000"), "10.0.0.5");
        Map<String, String> params = tachThamSo(result.redirectUrl());

        assertTrue(result.requiresRedirect());
        assertTrue(result.redirectUrl().startsWith(properties.getVnpay().getPayUrl() + "?"));
        assertEquals("DH001", params.get("vnp_TxnRef"));
        assertEquals("12500000", params.get("vnp_Amount"), "VNPAY tinh theo don vi x100");
        assertEquals("2.1.0", params.get("vnp_Version"));
        assertEquals("TESTCODE", params.get("vnp_TmnCode"));
        assertEquals("10.0.0.5", params.get("vnp_IpAddr"));
        assertNotNull(params.get("vnp_CreateDate"));
        assertNotNull(params.get("vnp_ExpireDate"));
        assertTrue(strategy.verifySignature(params), "Chu ky do chinh minh tao ra phai hop le");
    }

    @Test
    @DisplayName("Sai IP thi lay IP mac dinh, khong nem loi")
    void ipNullThiLayMacDinh() {
        cauHinh();
        Map<String, String> params = tachThamSo(strategy.initiate(donHang("DH002", "50000"), null).redirectUrl());
        assertEquals("127.0.0.1", params.get("vnp_IpAddr"));
    }

    @Test
    @DisplayName("Chu ky sai thi KHONG duoc coi la hop le")
    void chuKySaiBiTuChoi() {
        cauHinh();
        Map<String, String> params = tachThamSo(strategy.initiate(donHang("DH003", "100000"), "127.0.0.1").redirectUrl());

        assertTrue(strategy.verifySignature(params));

        // Gia mao so tien: doi 1 tham so nhung giu nguyen chu ky cu.
        Map<String, String> suaDoi = new LinkedHashMap<>(params);
        suaDoi.put("vnp_Amount", "100");
        assertFalse(strategy.verifySignature(suaDoi), "Sua so tien thi chu ky phai sai");

        Map<String, String> khongChuKy = new LinkedHashMap<>(params);
        khongChuKy.remove("vnp_SecureHash");
        assertFalse(strategy.verifySignature(khongChuKy), "Thieu chu ky phai bi tu choi");

        Map<String, String> chuKyBia = new LinkedHashMap<>(params);
        chuKyBia.put("vnp_SecureHash", "deadbeef");
        assertFalse(strategy.verifySignature(chuKyBia));
    }

    @Test
    @DisplayName("Doi khoa bi mat thi chu ky cu khong con hop le")
    void doiKhoaThiChuKyCuSai() {
        cauHinh();
        Map<String, String> params = tachThamSo(strategy.initiate(donHang("DH004", "100000"), "127.0.0.1").redirectUrl());
        properties.getVnpay().setHashSecret("SECRETKEYKHAC");
        assertFalse(strategy.verifySignature(params));
    }

    @Test
    @DisplayName("Doc ket qua tra ve: ma 00 la thanh cong, ma khac la that bai")
    void docKetQuaTraVe() {
        Map<String, String> ok = new LinkedHashMap<>();
        ok.put("vnp_ResponseCode", "00");
        ok.put("vnp_TransactionStatus", "00");
        ok.put("vnp_TxnRef", "DH005");
        ok.put("vnp_TransactionNo", "14123456");
        ok.put("vnp_Amount", "12500000");

        PaymentCallbackResult thanhCong = strategy.parseCallback(ok);
        assertTrue(thanhCong.success());
        assertEquals("DH005", thanhCong.transactionRef());
        assertEquals("14123456", thanhCong.gatewayTransactionId());
        assertEquals(0, new BigDecimal("125000").compareTo(thanhCong.amount()),
                "So tien tra ve phai duoc chia lai cho 100");

        Map<String, String> huy = new LinkedHashMap<>(ok);
        huy.put("vnp_ResponseCode", "24");
        assertFalse(strategy.parseCallback(huy).success());

        Map<String, String> thieuTrangThai = new LinkedHashMap<>(ok);
        thieuTrangThai.put("vnp_TransactionStatus", "02");
        assertFalse(strategy.parseCallback(thieuTrangThai).success(),
                "ResponseCode 00 nhung TransactionStatus khac 00 thi chua phai da thu tien");
    }

    @Test
    @DisplayName("So tien tra ve sai dinh dang thi tra ve null, khong nem loi")
    void soTienSaiDinhDang() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "DH006");
        params.put("vnp_Amount", "khong-phai-so");

        PaymentCallbackResult result = strategy.parseCallback(params);
        assertTrue(result.success());
        assertEquals(null, result.amount());
    }
}