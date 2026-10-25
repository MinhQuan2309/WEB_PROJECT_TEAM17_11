package com.utetra.module.auth;

import com.utetra.entity.OtpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gui ma OTP qua email.
 *
 * Neu chua khai bao spring.mail.username trong application-local.properties
 * thi KHONG gui mail that ma in ma OTP ra console. Nho vay ca nhom chay thu
 * duoc luong dang ky va quen mat khau ma khong can tai khoan Gmail.
 * Khi nop bai thi dien App Password cua Gmail vao la gui that.
 */
@Component
public class OtpSender {

    private static final Logger log = LoggerFactory.getLogger(OtpSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.otp.expire-minutes:5}")
    private int expireMinutes;

    public OtpSender(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean isMailConfigured() {
        return StringUtils.hasText(mailUsername) && mailSenderProvider.getIfAvailable() != null;
    }

    public void send(String toEmail, String fullName, String code, OtpType type) {
        String subject = type == OtpType.ACTIVATE_ACCOUNT
                ? "UTETra - Ma kich hoat tai khoan"
                : "UTETra - Ma dat lai mat khau";

        String body = buildBody(fullName, code, type);

        if (!isMailConfigured()) {
            log.warn("\n================== OTP (CHE DO THU NGHIEM) =================="
                    + "\nChua cau hinh spring.mail.username nen khong gui mail that."
                    + "\nEmail nhan : {}"
                    + "\nLoai ma    : {}"
                    + "\nMA OTP     : {}"
                    + "\nHet han sau: {} phut"
                    + "\n=============================================================",
                    toEmail, type, code, expireMinutes);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSenderProvider.getObject().send(message);
            log.info("Da gui OTP {} toi {}", type, toEmail);
        } catch (Exception ex) {
            // Khong de loi gui mail lam hong luong dang ky: ghi log de con doi soat.
            log.error("Gui mail OTP that bai toi {}: {}", toEmail, ex.getMessage());
            throw new IllegalStateException("Khong gui duoc email. Kiem tra lai cau hinh spring.mail.");
        }
    }

    private String buildBody(String fullName, String code, OtpType type) {
        String action = type == OtpType.ACTIVATE_ACCOUNT ? "kich hoat tai khoan" : "dat lai mat khau";
        return "Xin chao " + fullName + ",\n\n"
                + "Ma OTP de " + action + " tai UTETra la: " + code + "\n"
                + "Ma co hieu luc trong " + expireMinutes + " phut.\n\n"
                + "Neu ban khong yeu cau, hay bo qua email nay.\n\n"
                + "UTETra";
    }
}
