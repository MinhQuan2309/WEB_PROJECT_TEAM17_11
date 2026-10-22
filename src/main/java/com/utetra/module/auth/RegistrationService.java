package com.utetra.module.auth;

import com.utetra.entity.OtpToken;
import com.utetra.entity.OtpType;
import com.utetra.entity.Role;
import com.utetra.entity.User;
import com.utetra.module.auth.dto.RegisterRequest;
import com.utetra.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Dang ky tai khoan va kich hoat bang ma OTP gui qua email.
 *
 * Tai khoan moi luon o trang thai enabled = false, chua dang nhap duoc.
 * Chi khi nhap dung OTP thi moi duoc bat len. Mat khau luon ma hoa BCrypt.
 */
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpSender otpSender;

    public RegistrationService(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               OtpService otpService,
                               OtpSender otpSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.otpSender = otpSender;
    }

    @Transactional
    public User register(RegisterRequest form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();

        if (!form.passwordsMatch()) {
            throw new IllegalArgumentException("Hai lan nhap mat khau khong giong nhau.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ten dang nhap da co nguoi dung.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email nay da duoc dang ky.");
        }

        User user = new User();
        user.setUsername(username);
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(false);
        user.setLocked(false);
        User saved = userRepository.save(user);

        sendActivationOtp(saved);
        return saved;
    }

    /** Gui (hoac gui lai) ma kich hoat cho tai khoan chua bat. */
    @Transactional
    public void sendActivationOtp(User user) {
        if (user.isEnabled()) {
            throw new IllegalArgumentException("Tai khoan nay da duoc kich hoat.");
        }
        OtpToken token = otpService.issue(user, OtpType.ACTIVATE_ACCOUNT);
        otpSender.send(user.getEmail(), user.getFullName(), token.getCode(), OtpType.ACTIVATE_ACCOUNT);
    }

    @Transactional
    public void resendActivationOtp(String email) {
        sendActivationOtp(requireUserByEmail(email));
    }

    /** Nhap dung OTP thi bat tai khoan len. */
    @Transactional
    public User activate(String email, String code) {
        User user = requireUserByEmail(email);
        if (user.isEnabled()) {
            throw new IllegalArgumentException("Tai khoan nay da duoc kich hoat, ban co the dang nhap.");
        }
        otpService.consume(user, OtpType.ACTIVATE_ACCOUNT, code);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private User requireUserByEmail(String email) {
        return userRepository.findByEmail(email == null ? null : email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan voi email nay."));
    }
}
