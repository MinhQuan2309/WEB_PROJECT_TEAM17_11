package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Shop cua Vendor. Mot Vendor so huu mot Shop.
 * commissionRate = phan tram chiet khau san thu tren moi don, do Admin/Manager dat.
 */
@Entity
@Table(name = "shops")
public class Shop extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", unique = true)
    private User owner;

    /** Manager duoc phan cong quan ly shop nay. Null = chua phan cong. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    private String logoUrl;

    private String bannerUrl;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopStatus status = ShopStatus.PENDING;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("10.00");

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public ShopStatus getStatus() { return status; }
    public void setStatus(ShopStatus status) { this.status = status; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
}
