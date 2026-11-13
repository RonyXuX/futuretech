package com.futuretech.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futuretech.entity.RefreshToken;
import com.futuretech.entity.User;
import com.futuretech.exception.AuthException;
import com.futuretech.repository.RefreshTokenRepository;
import com.futuretech.support.MutableClock;
import com.futuretech.support.TestSecurity;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T12:00:00Z"));
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, TestSecurity.properties(), clock);
        user = new User();
        user.setId(5L);
    }

    @Test
    void create_storesOnlyTheHash_andExpiresIn7Days() {
        String raw = service.create(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getTokenHash()).hasSize(64);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(now().plusDays(7));
    }

    @Test
    void create_givesADifferentTokenEveryTime() {
        assertThat(service.create(user)).isNotEqualTo(service.create(user));
    }

    @Test
    void rotate_validToken_usesItUpAndReturnsANewOne() {
        RefreshToken stored = stored(now().plusDays(3));
        when(repository.findByTokenHash(RefreshTokenService.hash("old-token"))).thenReturn(Optional.of(stored));

        RefreshTokenService.Rotation rotation = service.rotate("old-token");

        assertThat(stored.getRevokedAt()).isEqualTo(now());        // old one can't be used again
        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.newRawToken()).isNotBlank().isNotEqualTo("old-token");
    }

    @Test
    void rotate_alreadyUsedToken_endsAllSessionsOfThatUser() {
        RefreshToken stolen = stored(now().plusDays(3));
        stolen.setRevokedAt(now().minusMinutes(5));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(AuthException.class);
        verify(repository).revokeAllForUser(eq(5L), any(LocalDateTime.class));
    }

    @Test
    void rotate_expiredToken_isRefused() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored(now().minusSeconds(1))));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(AuthException.class);
        verify(repository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void rotate_unknownToken_isRefused() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("made-up")).isInstanceOf(AuthException.class);
    }

    @Test
    void revoke_marksTokenAsUsed() {
        RefreshToken stored = stored(now().plusDays(3));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        service.revoke("some-token");

        assertThat(stored.isRevoked()).isTrue();
    }

    private RefreshToken stored(LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setCreatedAt(now().minus(Duration.ofDays(1)));
        token.setExpiresAt(expiresAt);
        return token;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
