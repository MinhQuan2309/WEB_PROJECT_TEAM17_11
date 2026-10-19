package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Nha van chuyen, do Admin/Manager quan ly (ten + phi van chuyen).
 */
@Entity
@Table(name = "carriers")
public class Carrier extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
