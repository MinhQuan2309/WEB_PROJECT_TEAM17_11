package com.utetra.module.auth.dto;

/** Ket qua tra ve cho client sau khi dang nhap bang API. */
public record LoginApiResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String username,
        String role) {

    public static LoginApiResponse of(String token, long expiresInMs, String username, String role) {
        return new LoginApiResponse(token, "Bearer", expiresInMs, username, role);
    }
}
