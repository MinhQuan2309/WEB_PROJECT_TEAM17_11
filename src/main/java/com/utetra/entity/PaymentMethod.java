package com.utetra.entity;

public enum PaymentMethod {
    COD("Thanh toan khi nhan hang"),
    VNPAY("VNPAY"),
    MOMO("MOMO");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
