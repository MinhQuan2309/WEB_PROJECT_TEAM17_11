package com.utetra.module.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tuan 1 chi co trang dang nhap (dung tai khoan mau cua DataSeeder).
 *
 * Tuan 2 Quan lam tiep tai day:
 *   - GET/POST /register          dang ky + sinh OTP
 *   - GET/POST /verify-otp        kich hoat tai khoan
 *   - GET/POST /forgot-password   gui OTP dat lai mat khau
 *   - GET/POST /reset-password    doi mat khau moi
 *   - POST     /api/auth/login    tra JWT cho phan API
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
