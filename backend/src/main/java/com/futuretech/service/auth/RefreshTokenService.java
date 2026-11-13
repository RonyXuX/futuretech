package com.futuretech.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.futuretech.config.SecurityProperties;
import com.futuretech.entity.RefreshToken;
import com.futuretech.entity.User;
import com.futuretech.exception.AuthException;
import com.futuretech.repository.RefreshTokenRepository;

/**
 * Refresh tokens: long random strings that let the browser get a new 15-minute JWT
 * without asking for the password again.
 *
 * - Only their SHA-256 hash is saved, so a database leak doesn't leak usable tokens.
 * - Rotation: each refresh "uses up" the token and hands out a new one.
 * - Reuse detection: if an already used token comes back, someone copied it. We then end
 *   ALL sessions of that user, so the thief's copy stops working too.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final SecurityProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    /** A user and the new raw refresh token that goes into their cookie. */
    public record Rotation(User user, String newRawToken) {
    }

    /** Creates and stores a new refresh token; returns the raw value (only the hash is saved). */
    public String create(User user) {
        byte[] bytes = new byte[32];  // 256 bits of randomness: impossible to guess
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(props.refreshTokenLifetime()));
        repository.save(token);
        return raw;
    }

    /** Uses up a refresh token and issues the next one. Must run inside a transaction. */
    public Rotation rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(AuthException::invalidRefreshToken);

        if (token.isRevoked()) {
            log.warn("Reused refresh token for user {}: ending all their sessions", token.getUser().getId());
            repository.revokeAllForUser(token.getUser().getId(), now);
            throw AuthException.invalidRefreshToken();
        }
        if (token.isExpiredAt(now)) {
            throw AuthException.invalidRefreshToken();
        }
        token.setRevokedAt(now);
        return new Rotation(token.getUser(), create(token.getUser()));
    }

    /** Logs the user out everywhere (e.g. after a password change). */
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, LocalDateTime.now(clock));
    }

    /** Logout: the token can't be used again. Unknown or already used tokens are ignored. */
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> token.setRevokedAt(LocalDateTime.now(clock)));
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
