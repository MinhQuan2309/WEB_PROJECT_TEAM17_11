package com.utetra.module.auth;

import com.utetra.module.auth.dto.RegisterRequest;
import com.utetra.module.auth.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Dang nhap, dang ky + OTP kich hoat, quen mat khau + dat lai mat khau.
 *
 * Tat ca duong dan o day da duoc permitAll trong SecurityConfig.
 */
@Controller
public class AuthController {

    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final OtpService otpService;
    private final OtpSender otpSender;

    public AuthController(RegistrationService registrationService,
                          PasswordResetService passwordResetService,
                          OtpService otpService,
                          OtpSender otpSender) {
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.otpService = otpService;
        this.otpSender = otpSender;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // ------------------------------------------------------------- dang ky

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form,
                           BindingResult bindingResult,
                           RedirectAttributes ra,
                           Model model) {
        if (!form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "mismatch",
                    "Hai lan nhap mat khau khong giong nhau");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            registrationService.register(form);
            ra.addFlashAttribute("successMessage",
                    "Da gui ma OTP toi email cua ban. Nhap ma de kich hoat tai khoan.");
            ra.addAttribute("email", form.getEmail().trim().toLowerCase());
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("formError", ex.getMessage());
            return "auth/register";
        }
    }

    // --------------------------------------------------------- kich hoat

    @GetMapping("/verify-otp")
    public String verifyForm(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("expireMinutes", otpService.getExpireMinutes());
        model.addAttribute("mailConfigured", otpSender.isMailConfigured());
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verify(@RequestParam String email,
                         @RequestParam String code,
                         RedirectAttributes ra) {
        try {
            registrationService.activate(email, code);
            ra.addFlashAttribute("successMessage",
                    "Kich hoat thanh cong. Ban co the dang nhap ngay bay gio.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addAttribute("email", email);
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/verify-otp/resend")
    public String resend(@RequestParam String email, RedirectAttributes ra) {
        try {
            registrationService.resendActivationOtp(email);
            ra.addFlashAttribute("successMessage", "Da gui lai ma OTP moi.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        ra.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

    // ------------------------------------------------------ quen mat khau

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email, RedirectAttributes ra) {
        passwordResetService.requestReset(email);
        ra.addFlashAttribute("successMessage",
                "Neu email ton tai trong he thong, ma OTP dat lai mat khau da duoc gui.");
        ra.addAttribute("email", email.trim().toLowerCase());
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String email, Model model) {
        if (!model.containsAttribute("form")) {
            ResetPasswordRequest form = new ResetPasswordRequest();
            form.setEmail(email);
            model.addAttribute("form", form);
        }
        model.addAttribute("expireMinutes", otpService.getExpireMinutes());
        model.addAttribute("mailConfigured", otpSender.isMailConfigured());
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("form") ResetPasswordRequest form,
                        BindingResult bindingResult,
                        RedirectAttributes ra,
                        Model model) {
        if (!form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "mismatch",
                    "Hai lan nhap mat khau khong giong nhau");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("expireMinutes", otpService.getExpireMinutes());
            return "auth/reset-password";
        }
        try {
            passwordResetService.resetPassword(form);
            ra.addFlashAttribute("successMessage",
                    "Doi mat khau thanh cong. Dang nhap bang mat khau moi.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("expireMinutes", otpService.getExpireMinutes());
            return "auth/reset-password";
        }
    }
}
