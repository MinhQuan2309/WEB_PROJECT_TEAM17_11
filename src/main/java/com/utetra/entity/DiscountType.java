package com.utetra.entity;

public enum DiscountType {
    PERCENT("Giam theo phan tram"),
    AMOUNT("Giam so tien"),
    FREE_SHIPPING("Mien phi van chuyen");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
