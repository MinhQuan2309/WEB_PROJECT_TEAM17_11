package com.utetra.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Du lieu form dang ky. Chi chua truong nguoi dung duoc phep nhap,
 * khong co role hay enabled de khong ai tu nang quyen qua form.
 */
public class RegisterRequest {

    @NotBlank(message = "Vui long nhap ten dang nhap")
    @Size(min = 4, max = 50, message = "Ten dang nhap tu 4 den 50 ky tu")
    private String username;

    @NotBlank(message = "Vui long nhap ho ten")
    @Size(max = 120, message = "Ho ten toi da 120 ky tu")
    private String fullName;

    @NotBlank(message = "Vui long nhap email")
    @Email(message = "Email khong dung dinh dang")
    @Size(max = 150)
    private String email;

    @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
    private String phone;

    @NotBlank(message = "Vui long nhap mat khau")
    @Size(min = 6, max = 100, message = "Mat khau toi thieu 6 ky tu")
    private String password;

    @NotBlank(message = "Vui long nhap lai mat khau")
    private String confirmPassword;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
