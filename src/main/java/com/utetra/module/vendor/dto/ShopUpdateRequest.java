package com.utetra.module.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Du lieu Vendor sua thong tin shop cua minh (tuan 1).
 *
 * Co tinh KHONG co status / commissionRate / owner / manager: Vendor khong duoc
 * tu doi trang thai shop hay chiet khau san, do la viec cua Admin/Manager.
 */
public class ShopUpdateRequest {

    @NotBlank(message = "Ten shop khong duoc de trong")
    @Size(max = 150, message = "Ten shop toi da 150 ky tu")
    private String name;

    @Size(max = 1000, message = "Mo ta toi da 1000 ky tu")
    private String description;

    @NotBlank(message = "Dia chi khong duoc de trong")
    @Size(max = 255, message = "Dia chi toi da 255 ky tu")
    private String address;

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