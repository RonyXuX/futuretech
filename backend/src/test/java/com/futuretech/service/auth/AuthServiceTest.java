package com.futuretech.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.futuretech.dto.auth.LoginRequest;
import com.futuretech.dto.auth.RegisterRequest;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.AuthException;
import com.futuretech.repository.UserRepository;
import com.futuretech.support.MutableClock;
import com.futuretech.support.TestSecurity;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String IP = "10.0.0.1";

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    // real BCrypt (strength 4 = fast enough for tests) so password checks are genuine
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T12:00:00Z"));
    private AuthService authService;
    private User lina;

    @BeforeEach
    void setUp() {
        // a generous rate limit, so only the per-account lock is tested here
        LoginRateLimiter limiter = new LoginRateLimiter(TestSecurity.properties(), clock) {
            @Override
            public void checkAllowed(String key) {
            }
        };
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService,
                limiter, TestSecurity.properties(), clock);

        lina = new User();
        lina.setId(1L);
        lina.setEmail("lina@example.com");
        lina.setPasswordHash(passwordEncoder.encode("Correct1pass"));
        lina.setFirstName("Lina");
        lina.setLastName("Haddad");
        lina.setRole(Role.CUSTOMER);

        lenient().when(jwtService.createAccessToken(any())).thenReturn("jwt-token");
        lenient().when(jwtService.accessTokenLifetimeSeconds()).thenReturn(900L);
        lenient().when(refreshTokenService.create(any())).thenReturn("refresh-token");
    }

    @Test
    void login_correctPassword_returnsBothTokens() {
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(lina));

        AuthService.AuthResult result = authService.login(new LoginRequest(" Lina@Example.com ", "Correct1pass"), IP);

        assertThat(result.response().accessToken()).isEqualTo("jwt-token");
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response().user().email()).isEqualTo("lina@example.com");
    }

    @Test
    void login_wrongPassword_isRejectedAndCounted() {
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(lina));

        assertThatThrownBy(() -> authService.login(new LoginRequest("lina@example.com", "wrong"), IP))
                .isInstanceOfSatisfying(AuthException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(lina.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_unknownEmail_givesTheSameErrorAsWrongPassword() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever1"), IP))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_fiveWrongPasswords_locksTheAccount() {
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(lina));
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("lina@example.com", "wrong"), IP))
                    .isInstanceOfSatisfying(AuthException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("lina@example.com", "wrong"), IP))
                .isInstanceOfSatisfying(AuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    void login_lockedAccount_refusesEvenTheCorrectPassword() {
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(lina));
        lina.setLockedUntil(java.time.LocalDateTime.now(clock).plusMinutes(10));

        assertThatThrownBy(() -> authService.login(new LoginRequest("lina@example.com", "Correct1pass"), IP))
                .isInstanceOfSatisfying(AuthException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.LOCKED));
        verify(jwtService, never()).createAccessToken(any());
    }

    @Test
    void login_afterTheLockEnds_correctPasswordWorksAndResetsCounter() {
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(lina));
        lina.setLockedUntil(java.time.LocalDateTime.now(clock).plusMinutes(15));
        clock.advance(Duration.ofMinutes(16));

        authService.login(new LoginRequest("lina@example.com", "Correct1pass"), IP);

        assertThat(lina.getLockedUntil()).isNull();
        assertThat(lina.getFailedLoginAttempts()).isZero();
    }

    @Test
    void register_createsACustomerWithAHashedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        authService.register(new RegisterRequest(" New@Example.com", "Secret123", "Sam", "Lee", ""), IP);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo("Secret123").startsWith("$2a$");
        assertThat(passwordEncoder.matches("Secret123", user.getPasswordHash())).isTrue();
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void register_existingEmail_returns409() {
        when(userRepository.existsByEmail("lina@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("lina@example.com", "Secret123", "L", "H", null), IP))
                .isInstanceOfSatisfying(AuthException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void register_sameEmailAtTheSameMoment_givesTheNormal409() {
        // both sign-ups passed existsByEmail; the database's UNIQUE constraint refuses the second one
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        RegisterRequest request = new RegisterRequest("new@example.com", "Secret123", "Sam", "Lee", null);

        assertThatThrownBy(() -> authService.register(request, IP))
                .isInstanceOfSatisfying(AuthException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void refresh_withoutCookie_isRefused() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(AuthException.class);
    }
}
