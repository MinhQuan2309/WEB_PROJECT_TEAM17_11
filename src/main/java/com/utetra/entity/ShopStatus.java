package com.utetra.entity;

public enum ShopStatus {
    PENDING("Cho duyet"),
    ACTIVE("Dang hoat dong"),
    SUSPENDED("Bi tam dung");

    private final String label;

    ShopStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
