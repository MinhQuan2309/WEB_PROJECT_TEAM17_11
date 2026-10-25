package com.utetra.module.payment.dto;

import java.math.BigDecimal;

/**
 * Ket qua doc callback / IPN tu cong thanh toan (tuan 4).
 *
 * @param success             cong thanh toan bao giao dich thanh cong
 * @param transactionRef      ma don hang phia cong (vnp_TxnRef / orderId)
 * @param gatewayTransactionId ma giao dich that cua cong (vnp_TransactionNo / transId)
 * @param responseCode        ma tra ve cua cong, giu lai de doi soat
 * @param amount              so tien cong bao da thu; duoc doi chieu voi tong tien don hang
 * @param message             mo ta ngan
 */
public record PaymentCallbackResult(boolean success,
                                    String transactionRef,
                                    String gatewayTransactionId,
                                    String responseCode,
                                    BigDecimal amount,
                                    String message) {

    public static PaymentCallbackResult failed(String responseCode, String message) {
        return new PaymentCallbackResult(false, null, null, responseCode, null, message);
    }
}