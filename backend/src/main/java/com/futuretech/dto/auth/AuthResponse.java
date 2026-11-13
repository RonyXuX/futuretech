package com.futuretech.dto.auth;

/** The refresh token is deliberately not in here: it is sent as an HttpOnly cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserDto user) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds, UserDto user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
