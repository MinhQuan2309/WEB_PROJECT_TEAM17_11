package com.utetra.entity;

/**
 * 5 vai trong de tai. Guest = chua dang nhap nen khong co trong enum.
 */
public enum Role {
    USER("Khach hang"),
    VENDOR("Nguoi ban"),
    MANAGER("Quan ly"),
    ADMIN("Quan tri he thong"),
    SHIPPER("Nguoi giao hang");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Dung cho Spring Security: ROLE_USER, ROLE_VENDOR... */
    public String authority() {
        return "ROLE_" + name();
    }
}
