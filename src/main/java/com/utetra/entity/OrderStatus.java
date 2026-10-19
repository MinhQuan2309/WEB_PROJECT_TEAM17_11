package com.utetra.entity;

/**
 * 7 trang thai don hang theo yeu cau de tai.
 */
public enum OrderStatus {
    NEW("Don hang moi"),
    CONFIRMED("Da xac nhan"),
    PICKED_UP("Da lay hang"),
    SHIPPING("Dang giao"),
    DELIVERED("Da giao"),
    CANCELLED("Da huy"),
    RETURNED_REFUNDED("Tra hang - hoan tien");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
