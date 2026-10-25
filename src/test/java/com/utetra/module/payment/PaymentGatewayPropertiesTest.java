package com.utetra.module.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra cau hinh cong thanh toan (tuan 4).
 *
 * Bai test nay nhu mot cai chot an toan: khoa that KHONG duoc nam trong ma nguon.
 * Ai do dan khoa vao gia tri mac dinh thi test nay do ngay.
 */
class PaymentGatewayPropertiesTest {

    @Test
    @DisplayName("Mac dinh KHONG co khoa that trong ma nguon")
    void macDinhKhongCoKhoaThat() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();

        assertEquals("", properties.getVnpay().getTmnCode());
        assertEquals("", properties.getVnpay().getHashSecret());
        assertEquals("", properties.getMomo().getPartnerCode());
        assertEquals("", properties.getMomo().getAccessKey());
        assertEquals("", properties.getMomo().getSecretKey());

        assertFalse(properties.getVnpay().isConfigured(), "Chua dien khoa thi phai coi la chua cau hinh");
        assertFalse(properties.getMomo().isConfigured());
    }

    @Test
    @DisplayName("Chi dia chi cong khai cua sandbox duoc dat san")
    void diaChiSandboxDatSan() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();

        assertEquals("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                properties.getVnpay().getPayUrl());
        assertEquals("2.1.0", properties.getVnpay().getVersion());
        assertEquals("https://test-payment.momo.vn/v2/gateway/api/create",
                properties.getMomo().getEndpoint());
        assertEquals("captureWallet", properties.getMomo().getRequestType());
    }

    @Test
    @DisplayName("Dien du khoa thi moi coi la da cau hinh")
    void dienDuKhoaThiDaCauHinh() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.getVnpay().setTmnCode("ABC");
        assertFalse(properties.getVnpay().isConfigured(), "Chi co tmn-code thi chua du");
        properties.getVnpay().setHashSecret("XYZ");
        assertTrue(properties.getVnpay().isConfigured());

        properties.getMomo().setPartnerCode("P");
        properties.getMomo().setAccessKey("A");
        assertFalse(properties.getMomo().isConfigured(), "Thieu secret-key thi chua du");
        properties.getMomo().setSecretKey("S");
        assertTrue(properties.getMomo().isConfigured());
    }
}