package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;

import java.util.Map;

/**
 * Mot cong thanh toan (tuan 4) - ap dung Strategy pattern theo dung yeu cau de tai.
 *
 * Moi cong (COD / VNPAY / MOMO) tu biet:
 *   - cach tao yeu cau thanh toan,
 *   - cach ky va cach kiem tra chu ky callback/IPN,
 *   - cach doc ket qua tra ve.
 * Nho vay PaymentService khong co mot cau if/else nao theo tung cong: them cong
 * thanh toan moi chi la them mot class implements interface nay.
 */
public interface PaymentStrategy {

    /** Cong thanh toan nay ung voi phuong thuc nao trong don hang. */
    PaymentMethod method();

    /** Ten hien thi cho nguoi dung. */
    String displayName();

    /** Da du cau hinh khoa that chua (COD luon true). */
    boolean isConfigured();

    /**
     * Tao yeu cau thanh toan cho don.
     *
     * @param clientIp IP cua khach, mot so cong bat buoc phai co
     */
    PaymentInitResult initiate(Order order, String clientIp);

    /**
     * Kiem tra chu ky cua du lieu callback/IPN.
     * Tra ve false khi thieu chu ky hoac chu ky sai: khi do KHONG duoc doi trang thai don.
     */
    boolean verifySignature(Map<String, String> params);

    /** Doc ket qua giao dich sau khi da kiem tra chu ky. */
    PaymentCallbackResult parseCallback(Map<String, String> params);
}