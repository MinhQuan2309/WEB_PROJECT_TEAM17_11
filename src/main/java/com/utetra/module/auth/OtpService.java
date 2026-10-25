package com.utetra.module.auth;

import com.utetra.entity.OtpToken;
import com.utetra.entity.OtpType;
import com.utetra.entity.User;
import com.utetra.repository.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Sinh va kiem tra ma OTP 6 chu so.
 *
 * Moi lan sinh ma moi thi ma cu cua cung loai bi danh dau da dung,
 * de khong ton tai hai ma song song cho mot nguoi.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    private final OtpTokenRepository otpTokenRepository;

    @Value("${app.otp.expire-minutes:5}")
    private int expireMinutes;

    public OtpService(OtpTokenRepository otpTokenRepository) {
        this.otpTokenRepository = otpTokenRepository;
    }

    /** Sinh ma moi, vo hieu hoa ma cu cung loai cua user. */
    @Transactional
    public OtpToken issue(User user, OtpType type) {
        otpTokenRepository.findTopByUserAndTypeAndUsedFalseOrderByIdDesc(user, type)
                .ifPresent(old -> {
                    old.setUsed(true);
                    otpTokenRepository.save(old);
                });

        OtpToken token = new OtpToken();
        token.setUser(user);
        token.setType(type);
        token.setCode(randomCode());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));
        token.setUsed(false);
        return otpTokenRepository.save(token);
    }

    /**
     * Kiem tra ma nguoi dung nhap. Dung thi danh dau da dung va tra ve true.
     *
     * @throws IllegalArgumentException khi ma sai, het han hoac da dung roi
     */
    @Transactional
    public void consume(User user, OtpType type, String inputCode) {
        OtpToken token = otpTokenRepository.findTopByUserAndTypeAndUsedFalseOrderByIdDesc(user, type)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chua co ma OTP nao duoc gui. Vui long bam gui lai ma."));

        if (token.isExpired()) {
            throw new IllegalArgumentException("Ma OTP da het han. Vui long bam gui lai ma.");
        }
        if (inputCode == null || !token.getCode().equals(inputCode.trim())) {
            throw new IllegalArgumentException("Ma OTP khong dung.");
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
