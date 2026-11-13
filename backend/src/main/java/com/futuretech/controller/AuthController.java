package com.futuretech.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.config.SecurityProperties;
import com.futuretech.dto.auth.AuthResponse;
import com.futuretech.dto.auth.LoginRequest;
import com.futuretech.dto.auth.RegisterRequest;
import com.futuretech.service.auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Login endpoints. The access token goes in the JSON body; the refresh token goes in a cookie:
 * - HttpOnly: JavaScript can't read it, so an injected script (XSS) can't steal it
 * - SameSite=Strict: the browser never sends it with requests started by other websites
 * - Path=/api/auth: it's only sent to these endpoints, not with every API call
 * - Secure (in production): only sent over HTTPS
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final SecurityProperties props;

    public AuthController(AuthService authService, SecurityProperties props) {
        this.authService = authService;
        this.props = props;
    }

    /** POST /api/auth/register - create a customer account (and log in). 201 Created. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        AuthService.AuthResult result = authService.register(body, request.getRemoteAddr());
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), result);
    }

    /** POST /api/auth/login - email + password. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        AuthService.AuthResult result = authService.login(body, request.getRemoteAddr());
        return withRefreshCookie(ResponseEntity.ok(), result);
    }

    /** POST /api/auth/refresh - new access token from the refresh cookie (the frontend calls this when the JWT expires). */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return withRefreshCookie(ResponseEntity.ok(), result);
    }

    /** POST /api/auth/logout - ends the session and deletes the cookie. 204 No Content. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie expired = refreshCookie("").maxAge(0).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expired.toString()).build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(ResponseEntity.BodyBuilder builder, AuthService.AuthResult result) {
        ResponseCookie cookie = refreshCookie(result.refreshToken()).maxAge(props.refreshTokenLifetime()).build();
        return builder.header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.response());
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/api/auth");
    }
}
