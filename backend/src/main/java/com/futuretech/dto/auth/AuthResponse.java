package com.futuretech.dto.auth;

/**
 * Returned by register, login and refresh. The frontend keeps the access token in memory and
 * sends it as "Authorization: Bearer <accessToken>". The refresh token is NOT in this body:
 * it is set as an httpOnly cookie that JavaScript cannot read.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserDto user) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds, UserDto user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
