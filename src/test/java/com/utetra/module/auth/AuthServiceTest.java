package com.utetra.module.auth;

import com.utetra.entity.OtpToken;
import com.utetra.entity.OtpType;
import com.utetra.entity.Role;
import com.utetra.entity.User;
import com.utetra.module.auth.dto.RegisterRequest;
import com.utetra.module.auth.dto.ResetPasswordRequest;
import com.utetra.repository.OtpTokenRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dang ky + OTP kich hoat + quen mat khau.
 *
 * Diem quan trong: tai khoan moi phai chua kich hoat, mat khau luon BCrypt,
 * va ma OTP sai / het han / dung lai deu phai bi tu choi.
 */
@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired RegistrationService registrationService;
    @Autowired PasswordResetService passwordResetService;
    @Autowired OtpService otpService;
    @Autowired UserRepository userRepository;
    @Autowired OtpTokenRepository otpTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private RegisterRequest form(String username, String email) {
        RegisterRequest f = new RegisterRequest();
        f.setUsername(username);
        f.setFullName("Nguoi Dung Test");
        f.setEmail(email);
        f.setPhone("0900000000");
        f.setPassword("123456");
        f.setConfirmPassword("123456");
        return f;
    }

    private String currentCode(User user, OtpType type) {
        return otpTokenRepository.findTopByUserAndTypeAndUsedFalseOrderByIdDesc(user, type)
                .orElseThrow().getCode();
    }

    // ------------------------------------------------------------ dang ky

    @Test
    void dangKyXongTaiKhoanChuaKichHoatVaMatKhauDaBcrypt() {
        User user = registrationService.register(form("nguoimoi1", "nguoimoi1@utetra.test"));

        assertFalse(user.isEnabled(), "Tai khoan moi phai o trang thai chua kich hoat");
        assertEquals(Role.USER, user.getRole(), "Dang ky qua form luon la vai USER");
        assertTrue(user.getPassword().startsWith("$2"), "Mat khau phai la chuoi BCrypt");
        assertTrue(passwordEncoder.matches("123456", user.getPassword()));
        assertNotEquals("123456", user.getPassword());
    }

    @Test
    void khongDangKyTrungTenDangNhap() {
        registrationService.register(form("trungten", "trungten@utetra.test"));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.register(form("trungten", "khac@utetra.test")));
        assertTrue(ex.getMessage().contains("Ten dang nhap"));
    }

    @Test
    void khongDangKyTrungEmail() {
        registrationService.register(form("nguoia", "trungmail@utetra.test"));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.register(form("nguoib", "trungmail@utetra.test")));
        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    void haiLanNhapMatKhauKhacNhauThiBiTuChoi() {
        RegisterRequest f = form("lechmk", "lechmk@utetra.test");
        f.setConfirmPassword("999999");

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(f));
    }

    // ----------------------------------------------------------- kich hoat

    @Test
    void nhapDungOtpThiTaiKhoanDuocKichHoat() {
        User user = registrationService.register(form("kichhoat", "kichhoat@utetra.test"));
        String code = currentCode(user, OtpType.ACTIVATE_ACCOUNT);

        User activated = registrationService.activate("kichhoat@utetra.test", code);

        assertTrue(activated.isEnabled());
    }

    @Test
    void nhapSaiOtpThiKhongKichHoat() {
        User user = registrationService.register(form("saiotp", "saiotp@utetra.test"));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.activate("saiotp@utetra.test", "000000"));
        assertTrue(ex.getMessage().contains("khong dung"));
        assertFalse(userRepository.findById(user.getId()).orElseThrow().isEnabled());
    }

    @Test
    void otpHetHanThiBiTuChoi() {
        User user = registrationService.register(form("hethan", "hethan@utetra.test"));
        OtpToken token = otpTokenRepository
                .findTopByUserAndTypeAndUsedFalseOrderByIdDesc(user, OtpType.ACTIVATE_ACCOUNT).orElseThrow();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpTokenRepository.save(token);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.activate("hethan@utetra.test", token.getCode()));
        assertTrue(ex.getMessage().contains("het han"));
    }

    @Test
    void khongDungLaiDuocOtpDaDung() {
        User user = registrationService.register(form("dungmotlan", "dungmotlan@utetra.test"));
        String code = currentCode(user, OtpType.ACTIVATE_ACCOUNT);
        registrationService.activate("dungmotlan@utetra.test", code);

        // Tai khoan da bat roi, dung lai ma cu phai bi chan
        assertThrows(IllegalArgumentException.class,
                () -> registrationService.activate("dungmotlan@utetra.test", code));
    }

    @Test
    void guiLaiOtpThiMaCuHetHieuLuc() {
        User user = registrationService.register(form("guilai", "guilai@utetra.test"));
        String maCu = currentCode(user, OtpType.ACTIVATE_ACCOUNT);

        registrationService.resendActivationOtp("guilai@utetra.test");
        String maMoi = currentCode(user, OtpType.ACTIVATE_ACCOUNT);

        assertNotEquals(maCu, maMoi, "Gui lai phai sinh ma khac");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.activate("guilai@utetra.test", maCu));
        assertTrue(ex.getMessage().contains("khong dung"));
    }

    // ------------------------------------------------------ quen mat khau

    @Test
    void datLaiMatKhauBangOtpThanhCong() {
        User user = registrationService.register(form("quenmk", "quenmk@utetra.test"));
        registrationService.activate("quenmk@utetra.test", currentCode(user, OtpType.ACTIVATE_ACCOUNT));

        passwordResetService.requestReset("quenmk@utetra.test");
        String code = currentCode(user, OtpType.RESET_PASSWORD);

        ResetPasswordRequest f = new ResetPasswordRequest();
        f.setEmail("quenmk@utetra.test");
        f.setCode(code);
        f.setPassword("matkhaumoi");
        f.setConfirmPassword("matkhaumoi");
        passwordResetService.resetPassword(f);

        User sau = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("matkhaumoi", sau.getPassword()));
        assertFalse(passwordEncoder.matches("123456", sau.getPassword()), "Mat khau cu phai het tac dung");
    }

    @Test
    void emailKhongTonTaiThiKhongBaoLoi() {
        // Khong duoc de lo email nao da dang ky, nen van im lang coi nhu thanh cong
        passwordResetService.requestReset("khongcoai@utetra.test");
    }

    @Test
    void datLaiMatKhauVoiOtpSaiThiBiTuChoi() {
        User user = registrationService.register(form("saimareset", "saimareset@utetra.test"));
        passwordResetService.requestReset("saimareset@utetra.test");

        ResetPasswordRequest f = new ResetPasswordRequest();
        f.setEmail("saimareset@utetra.test");
        f.setCode("123123");
        f.setPassword("matkhaumoi");
        f.setConfirmPassword("matkhaumoi");

        assertThrows(IllegalArgumentException.class, () -> passwordResetService.resetPassword(f));
        assertTrue(passwordEncoder.matches("123456",
                userRepository.findById(user.getId()).orElseThrow().getPassword()));
    }

    @Test
    void maOtpLaSauChuSo() {
        User user = registrationService.register(form("dodai", "dodai@utetra.test"));
        String code = currentCode(user, OtpType.ACTIVATE_ACCOUNT);

        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }
}
