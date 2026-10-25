package com.utetra.module.auth;

import com.utetra.entity.OtpToken;
import com.utetra.entity.OtpType;
import com.utetra.entity.User;
import com.utetra.module.auth.dto.ResetPasswordRequest;
import com.utetra.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Quen mat khau: gui OTP qua email roi dat lai mat khau moi.
 *
 * Luu y ve bao mat: khi email khong ton tai thi van bao thanh cong,
 * de nguoi la khong do duoc email nao da dang ky tren he thong.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpSender otpSender;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                OtpService otpService,
                                OtpSender otpSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.otpSender = otpSender;
    }

    /** Gui OTP dat lai mat khau. Khong bao cho nguoi goi biet email co ton tai hay khong. */
    @Transactional
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(normalize(email));
        if (found.isEmpty()) {
            log.info("Yeu cau dat lai mat khau cho email khong ton tai, bo qua am tham.");
            return;
        }
        User user = found.get();
        OtpToken token = otpService.issue(user, OtpType.RESET_PASSWORD);
        otpSender.send(user.getEmail(), user.getFullName(), token.getCode(), OtpType.RESET_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest form) {
        if (!form.passwordsMatch()) {
            throw new IllegalArgumentException("Hai lan nhap mat khau khong giong nhau.");
        }
        User user = userRepository.findByEmail(normalize(form.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Ma OTP khong dung."));

        otpService.consume(user, OtpType.RESET_PASSWORD, form.getCode());

        user.setPassword(passwordEncoder.encode(form.getPassword()));
        // Dat lai mat khau thanh cong thi mo khoa luon tai khoan chua kich hoat.
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
