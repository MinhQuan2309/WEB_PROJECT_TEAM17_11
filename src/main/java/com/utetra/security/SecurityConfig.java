package com.utetra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Phan quyen 5 vai theo de tai.
 *
 *   /                 /products/**  /shop/**   : Guest xem duoc
 *   /account /cart /checkout /orders /review   : phai dang nhap (USER tro len)
 *   /vendor/**                                 : VENDOR
 *   /shipper/**                                : SHIPPER
 *   /manager/**                                : MANAGER
 *   /admin/**                                  : ADMIN
 *
 * Giao dien web dung session, phan /api/** dung JWT (JwtAuthenticationFilter).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          LoginSuccessHandler loginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF bat cho form web, tat cho API vi API dung JWT
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/ws/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/template-bootstrap/**",
                                 "/favicon.ico", "/webjars/**").permitAll()
                .requestMatchers("/", "/home", "/products/**", "/shop/**", "/search",
                                 "/category/**", "/ws/**").permitAll()
                .requestMatchers("/login", "/logout", "/register/**",
                                 "/forgot-password/**", "/reset-password/**",
                                 "/verify-otp/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/vendor/**").hasAnyRole("ADMIN", "VENDOR")
                .requestMatchers("/shipper/**").hasAnyRole("ADMIN", "SHIPPER")
                .requestMatchers("/account/**", "/cart/**", "/checkout/**",
                                 "/orders/**", "/review/**", "/wishlist/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Mat khau luon duoc ma hoa BCrypt theo yeu cau de tai. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
