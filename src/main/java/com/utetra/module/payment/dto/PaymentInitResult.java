package com.utetra.module.payment.dto;

/**
 * Ket qua buoc "bat dau thanh toan" (tuan 4).
 *
 * @param redirectUrl    URL de chuyen trinh duyet sang cong thanh toan; null neu khong can
 *                       chuyen huong (vi du COD)
 * @param transactionRef ma don hang phia cong thanh toan (vnp_TxnRef / orderId cua MOMO)
 * @param message        loi nhan hien cho nguoi dung
 */
public record PaymentInitResult(String redirectUrl, String transactionRef, String message) {

    public static PaymentInitResult redirect(String redirectUrl, String transactionRef, String message) {
        return new PaymentInitResult(redirectUrl, transactionRef, message);
    }

    public static PaymentInitResult noRedirect(String transactionRef, String message) {
        return new PaymentInitResult(null, transactionRef, message);
    }

    public boolean requiresRedirect() {
        return redirectUrl != null && !redirectUrl.isBlank();
    }
}