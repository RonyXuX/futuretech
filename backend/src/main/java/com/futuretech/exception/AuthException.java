package com.futuretech.exception;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/**
 * Every login/registration failure, each with the HTTP status it becomes.
 * retryAfter is set when the client must wait (sent as the "Retry-After" header).
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final Duration retryAfter;

    private AuthException(HttpStatus status, String message, Duration retryAfter) {
        super(message);
        this.status = status;
        this.retryAfter = retryAfter;
    }

    /** Same message for "no such email" and "wrong password", so attackers can't tell which. */
    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password", null);
    }

    public static AuthException accountLocked(Duration retryAfter) {
        return new AuthException(HttpStatus.LOCKED,
                "Too many failed logins. This account is locked, try again later.", retryAfter);
    }

    public static AuthException tooManyAttempts(Duration retryAfter) {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many attempts from your network. Please wait a minute.", retryAfter);
    }

    public static AuthException emailAlreadyUsed() {
        return new AuthException(HttpStatus.CONFLICT, "An account with this email already exists", null);
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again", null);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
