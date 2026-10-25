package com.utetra.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Du lieu form dat lai mat khau bang ma OTP gui qua email. */
public class ResetPasswordRequest {

    @NotBlank(message = "Vui long nhap email")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotBlank(message = "Vui long nhap ma OTP")
    private String code;

    @NotBlank(message = "Vui long nhap mat khau moi")
    @Size(min = 6, max = 100, message = "Mat khau toi thieu 6 ky tu")
    private String password;

    @NotBlank(message = "Vui long nhap lai mat khau moi")
    private String confirmPassword;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
