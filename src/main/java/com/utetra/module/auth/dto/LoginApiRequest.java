package com.utetra.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body cua POST /api/auth/login. */
public record LoginApiRequest(
        @NotBlank(message = "Vui long nhap ten dang nhap") String username,
        @NotBlank(message = "Vui long nhap mat khau") String password) {
}
