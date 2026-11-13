package com.futuretech.service.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.config.SecurityProperties;
import com.futuretech.dto.auth.AuthResponse;
import com.futuretech.dto.auth.LoginRequest;
import com.futuretech.dto.auth.RegisterRequest;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.AuthException;
import com.futuretech.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;
    private final SecurityProperties props;
    private final Clock clock;

    // Checked when the email doesn't exist, so "unknown email" takes as long as "wrong password"
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokenService, LoginRateLimiter rateLimiter,
                       SecurityProperties props, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    public record AuthResult(AuthResponse response, String refreshToken) {
    }

    /** Always a CUSTOMER: admins are never created through the API. */
    @Transactional
    public AuthResult register(RegisterRequest request, String clientIp) {
        rateLimiter.checkAllowed("register:" + clientIp);
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw AuthException.emailAlreadyUsed();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
        return issueTokens(user);
    }

    // noRollbackFor: the failed-attempt counter must be saved even though we throw
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResult login(LoginRequest request, String clientIp) {
        rateLimiter.checkAllowed("login:" + clientIp);
        LocalDateTime now = LocalDateTime.now(clock);

        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw AuthException.invalidCredentials();
        }
        if (user.isLockedAt(now)) {
            throw AuthException.accountLocked(Duration.between(now, user.getLockedUntil()));
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now, props.maxFailedLogins(), props.lockDuration());
            if (user.isLockedAt(now)) {
                throw AuthException.accountLocked(props.lockDuration());
            }
            throw AuthException.invalidCredentials();
        }
        user.recordSuccessfulLogin();
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = AuthException.class)  // keep "end all sessions" after token reuse
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.invalidRefreshToken();
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = rotation.user();
        AuthResponse body = AuthResponse.bearer(jwtService.createAccessToken(user),
                jwtService.accessTokenLifetimeSeconds(), UserDto.from(user));
        return new AuthResult(body, rotation.newRawToken());
    }

    // The access token can't be revoked; it simply expires within 15 minutes
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthResult issueTokens(User user) {
        AuthResponse body = AuthResponse.bearer(jwtService.createAccessToken(user),
                jwtService.accessTokenLifetimeSeconds(), UserDto.from(user));
        return new AuthResult(body, refreshTokenService.create(user));
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
