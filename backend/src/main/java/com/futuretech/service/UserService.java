package com.futuretech.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.auth.ChangePasswordRequest;
import com.futuretech.dto.auth.UpdateProfileRequest;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.User;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.UserRepository;
import com.futuretech.service.auth.RefreshTokenService;

/** The logged-in user's own account. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return UserDto.from(load(id));
    }

    @Transactional
    public UserDto updateProfile(Long id, UpdateProfileRequest request) {
        User user = load(id);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return UserDto.from(user);  // saved automatically at the end of the transaction
    }

    /**
     * Needs the current password, so someone using a forgotten-open laptop can't take over the
     * account. Afterwards every refresh token is revoked: all devices must log in again.
     */
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = load(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("currentPassword", "is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("newPassword", "must be different from your current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(id);
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }
}
