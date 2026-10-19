package com.utetra.module.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Du lieu form dang ky shop (tuan 1).
 *
 * CHI chua cac truong Vendor duoc phep gui len. Cac truong quan trong nhu
 * owner, status, commissionRate, manager do Admin/Manager dat, nen khong dua vao DTO
 * (chong mass assignment: nguoi dung khong the tu gan status=ACTIVE tu form).
 */
public class ShopRegistrationRequest {

    @NotBlank(message = "Ten shop khong duoc de trong")
    @Size(max = 150, message = "Ten shop toi da 150 ky tu")
    private String name;

    @Size(max = 1000, message = "Mo ta toi da 1000 ky tu")
    private String description;

    @NotBlank(message = "Dia chi khong duoc de trong")
    @Size(max = 255, message = "Dia chi toi da 255 ky tu")
    private String address;

    /** Cho phep de trong; neu dien thi phai dung dinh dang so dien thoai Viet Nam. */
    @Pattern(regexp = "^$|^(0|\\+84)[0-9]{8,10}$", message = "So dien thoai khong hop le")
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}