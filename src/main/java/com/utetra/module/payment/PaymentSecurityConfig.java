package com.utetra.module.payment;

import com.utetra.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Chuoi loc rieng cho /payment/** (tuan 4).
 *
 * Vi sao can: SecurityConfig cua nhom ket thuc bang anyRequest().authenticated(), nen
 * IPN (server VNPAY/MOMO goi vao, khong co phien dang nhap) se bi day ve trang login.
 * Thay vi sua file dung chung SecurityConfig, module nay tu khai bao mot chuoi loc
 * chi ap dung cho /payment/** va duoc uu tien chay truoc (@Order(1)).
 *
 * Quy tac:
 *   - /payment/vnpay/return, /payment/momo/return : khach quay ve tu cong -> cho phep
 *     (khong dang nhap van xem duoc ket qua, nhung khong thay duoc don cua nguoi khac
 *     vi trang ket qua van kiem tra chu so huu).
 *   - /payment/vnpay/ipn, /payment/momo/ipn       : cong goi server -> cho phep, va tat
 *     CSRF vi cong khong co token cua minh. Bao ve bang chu ky HMAC chu khong bang CSRF.
 *   - Cac duong dan con lai (/payment/checkout/**, /payment/start/**): phai dang nhap.
 */
@Configuration
public class PaymentSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public PaymentSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain paymentFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/payment/**")
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/payment/vnpay/ipn", "/payment/momo/ipn"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/payment/vnpay/return", "/payment/vnpay/ipn",
                                 "/payment/momo/return", "/payment/momo/ipn").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}