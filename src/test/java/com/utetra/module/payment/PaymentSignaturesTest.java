package com.utetra.module.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra ham ky dung chung (tuan 4).
 * Dung vector chuan cua HMAC-SHA256 de chac chan khong bi sai thuat toan.
 */
class PaymentSignaturesTest {

    @Test
    @DisplayName("HMAC-SHA256 dung vector chuan")
    void hmacSha256DungVectorChuan() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                PaymentSignatures.hmacSha256("key", "The quick brown fox jumps over the lazy dog"));
    }

    @Test
    @DisplayName("HMAC-SHA512 tra ve 128 ky tu hex va doi khi doi khoa")
    void hmacSha512DungDinhDang() {
        String a = PaymentSignatures.hmacSha512("secret", "vnp_Amount=100");
        String b = PaymentSignatures.hmacSha512("secret", "vnp_Amount=100");
        String c = PaymentSignatures.hmacSha512("secret-khac", "vnp_Amount=100");

        assertEquals(128, a.length(), "HMAC-SHA512 la 64 byte = 128 ky tu hex");
        assertEquals(a, b, "Cung du lieu + cung khoa thi chu ky phai giong nhau");
        assertFalse(a.equals(c), "Doi khoa thi chu ky phai khac");
    }

    @Test
    @DisplayName("Ma hoa giong ban mau VNPAY: khoang trang thanh dau + chu khong phai %20")
    void maHoaGiongVnpay() {
        assertEquals("Thanh+toan+don+hang", PaymentSignatures.urlEncode("Thanh toan don hang"));
        assertEquals("a%2Fb%3Dc", PaymentSignatures.urlEncode("a/b=c"));
    }

    @Test
    @DisplayName("Sap xep tham so theo ten va bo tham so rong / chu ky")
    void sapXepVaBoThamSo() {
        Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "DH1");
        params.put("vnp_Amount", "100");
        params.put("vnp_SecureHash", "abc");
        params.put("vnp_Empty", "");

        TreeMap<String, String> sorted = PaymentSignatures.sortedParams(params, "vnp_SecureHash");

        assertEquals("[vnp_Amount, vnp_TxnRef]", sorted.keySet().toString());
        assertEquals("vnp_Amount=100&vnp_TxnRef=DH1",
                PaymentSignatures.buildVnpayHashData(sorted));
    }

    @Test
    @DisplayName("Chuoi ky MOMO khong ma hoa gia tri")
    void chuoiKyMomoKhongMaHoa() {
        TreeMap<String, String> data = new TreeMap<>();
        data.put("orderInfo", "Thanh toan don hang");
        data.put("amount", "1000");

        assertEquals("amount=1000&orderInfo=Thanh toan don hang",
                PaymentSignatures.buildMomoRawData(data));
    }

    @Test
    @DisplayName("So sanh chu ky khong lo thoi gian: van dung va an toan voi null")
    void soSanhChuKy() {
        assertTrue(PaymentSignatures.constantTimeEquals("abc", "abc"));
        assertFalse(PaymentSignatures.constantTimeEquals("abc", "abd"));
        assertFalse(PaymentSignatures.constantTimeEquals("abc", "abcd"));
        assertFalse(PaymentSignatures.constantTimeEquals(null, "abc"));
        assertFalse(PaymentSignatures.constantTimeEquals("abc", null));
    }
}