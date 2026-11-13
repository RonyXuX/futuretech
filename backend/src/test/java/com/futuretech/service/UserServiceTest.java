package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.futuretech.dto.auth.ChangePasswordRequest;
import com.futuretech.dto.auth.UpdateProfileRequest;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.User;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.repository.UserRepository;
import com.futuretech.service.auth.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, refreshTokenService);
        user = new User();
        user.setId(5L);
        user.setEmail("lina@example.com");
        user.setFirstName("Lina");
        user.setLastName("Haddad");
        user.setPasswordHash(passwordEncoder.encode("OldPass123"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
    }

    @Test
    void updateProfile_trimsValues_andBlankPhoneBecomesEmpty() {
        UserDto result = userService.updateProfile(5L, new UpdateProfileRequest("  Lina ", " Khoury", "   "));

        assertThat(result.firstName()).isEqualTo("Lina");
        assertThat(result.lastName()).isEqualTo("Khoury");
        assertThat(user.getPhone()).isNull();
        assertThat(result.email()).isEqualTo("lina@example.com");  // email never changes here
    }

    @Test
    void changePassword_wrongCurrentPassword_isRefused() {
        assertThatThrownBy(() -> userService.changePassword(5L, new ChangePasswordRequest("guess", "NewPass123")))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("currentPassword"));
        verify(refreshTokenService, never()).revokeAllForUser(5L);
    }

    @Test
    void changePassword_sameAsBefore_isRefused() {
        assertThatThrownBy(() -> userService.changePassword(5L, new ChangePasswordRequest("OldPass123", "OldPass123")))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("newPassword"));
    }

    @Test
    void changePassword_success_storesNewHash_andLogsOutEverywhere() {
        userService.changePassword(5L, new ChangePasswordRequest("OldPass123", "NewPass123"));

        assertThat(passwordEncoder.matches("NewPass123", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldPass123", user.getPasswordHash())).isFalse();
        verify(refreshTokenService).revokeAllForUser(5L);
    }
}
