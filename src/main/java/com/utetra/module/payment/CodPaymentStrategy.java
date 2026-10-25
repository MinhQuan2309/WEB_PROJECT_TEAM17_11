package com.utetra.module.payment;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentMethod;
import com.utetra.module.payment.dto.PaymentCallbackResult;
import com.utetra.module.payment.dto.PaymentInitResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Thanh toan khi nhan hang (COD) - tuan 4.
 *
 * COD khong co cong thanh toan nao o giua: tien duoc shipper thu luc giao hang,
 * nen khong co URL chuyen huong va cung khong co callback/IPN. Trang thai thanh toan
 * cua don COD duoc chuyen thanh PAID khi don duoc danh dau da giao
 * (xem OrderStatusService).
 */
@Component
public class CodPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public String displayName() {
        return "Thanh toan khi nhan hang (COD)";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public PaymentInitResult initiate(Order order, String clientIp) {
        return PaymentInitResult.noRedirect(order.getCode(),
                "Don " + order.getCode() + " se thanh toan khi nhan hang.");
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        // COD khong co du lieu tu cong thanh toan nen khong bao gio co chu ky hop le.
        return false;
    }

    @Override
    public PaymentCallbackResult parseCallback(Map<String, String> params) {
        throw new UnsupportedOperationException("COD khong co callback tu cong thanh toan");
    }
}