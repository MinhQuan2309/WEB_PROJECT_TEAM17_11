package com.utetra.module.auth;

import com.utetra.entity.User;
import com.utetra.module.auth.dto.LoginApiRequest;
import com.utetra.module.auth.dto.LoginApiResponse;
import com.utetra.repository.UserRepository;
import com.utetra.security.CustomUserDetailsService;
import com.utetra.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Dang nhap bang API, tra ve JWT cho phan goi API va WebSocket.
 *
 *   POST /api/auth/login
 *   { "username": "khach", "password": "123456" }
 *
 * Giao dien web van dung session, khong dung token nay.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthApiController(AuthenticationManager authenticationManager,
                             CustomUserDetailsService userDetailsService,
                             JwtService jwtService,
                             UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginApiResponse> login(@Valid @RequestBody LoginApiRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tai khoan chua kich hoat. Vui long nhap ma OTP gui qua email.");
        } catch (LockedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan da bi khoa.");
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai ten dang nhap hoac mat khau.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow();

        return ResponseEntity.ok(LoginApiResponse.of(
                token, expirationMs, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(Object::toString).toList());
    }
}
