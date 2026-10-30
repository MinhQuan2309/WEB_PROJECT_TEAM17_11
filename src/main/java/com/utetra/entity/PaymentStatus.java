package com.utetra.entity;

/**
 * Trang thai thanh toan cua don hang.
 *
 * Don COD giu PENDING cho toi khi shipper giao xong; don tra truoc chuyen PAID
 * ngay sau khi cong thanh toan xac nhan. Huy don hoac tra hang ma da tra tien
 * thi chuyen REFUNDED.
 */
public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thanh toán thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
