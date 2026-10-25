package com.utetra.module.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ham dung chung cho chu ky cong thanh toan (tuan 4).
 *
 * Diem quan trong nhat: luc TAO chu ky va luc KIEM TRA chu ky phai dung y nguyen
 * cung mot cach ma hoa va cung mot thu tu tham so. Lop nay gom hai viec do lai
 * mot cho de khong bao gio lech nhau.
 */
final class PaymentSignatures {

    private PaymentSignatures() {
    }

    static String hmacSha512(String key, String data) {
        return hmac("HmacSHA512", key, data);
    }

    static String hmacSha256(String key, String data) {
        return hmac("HmacSHA256", key, data);
    }

    private static String hmac(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Khong tao duoc chu ky " + algorithm, ex);
        }
    }

    /**
     * Ma hoa theo RFC 3986 (khoang trang thanh %20).
     * VNPAY/MOMO deu tinh chu ky tren chuoi da ma hoa, nen phai dung chung ham nay.
     */
    static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Sap xep tham so theo ten (TreeMap) va bo cac tham so rong / chu ky. */
    static TreeMap<String, String> sortedParams(Map<String, String> params, String... loaiBo) {
        TreeMap<String, String> sorted = new TreeMap<>();
        if (params == null) {
            return sorted;
        }
        java.util.Set<String> boQua = java.util.Set.of(loaiBo);
        params.forEach((key, value) -> {
            if (key == null || value == null || value.isEmpty() || boQua.contains(key)) {
                return;
            }
            sorted.put(key, value);
        });
        return sorted;
    }

    /** Chuoi du lieu de ky kieu VNPAY: key=value&key=value, da ma hoa, da sap xep. */
    static String buildVnpayHashData(TreeMap<String, String> sortedParams) {
        StringBuilder data = new StringBuilder();
        sortedParams.forEach((key, value) -> {
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(urlEncode(key)).append('=').append(urlEncode(value));
        });
        return data.toString();
    }

    /** Chuoi du lieu de ky kieu MOMO: key=value&key=value, gia tri KHONG ma hoa. */
    static String buildMomoRawData(TreeMap<String, String> sortedParams) {
        StringBuilder data = new StringBuilder();
        sortedParams.forEach((key, value) -> {
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(key).append('=').append(value);
        });
        return data.toString();
    }

    /**
     * So sanh chu ky theo cach khong lo thoi gian (chong tan cong do thoi gian):
     * khong dung equals vi equals dung som khi gap ky tu khac nhau.
     */
    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}