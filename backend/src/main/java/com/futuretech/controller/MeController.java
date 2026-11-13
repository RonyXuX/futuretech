package com.futuretech.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.account.PointsHistoryDto;
import com.futuretech.dto.account.WarrantyDto;
import com.futuretech.dto.auth.ChangePasswordRequest;
import com.futuretech.dto.auth.UpdateProfileRequest;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.service.PointsService;
import com.futuretech.service.UserService;
import com.futuretech.service.WarrantyService;

import jakarta.validation.Valid;

/**
 * Endpoints about the logged-in user. The user id comes from the verified JWT ("sub" claim),
 * never from the URL, so nobody can read another person's data by changing an id.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final PointsService pointsService;
    private final WarrantyService warrantyService;

    public MeController(UserService userService, PointsService pointsService, WarrantyService warrantyService) {
        this.userService = userService;
        this.pointsService = pointsService;
        this.warrantyService = warrantyService;
    }

    /** GET /api/me - who am I? Needs "Authorization: Bearer <accessToken>". */
    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(userId(jwt));
    }

    /** PUT /api/me - change name and phone. */
    @PutMapping
    public UserDto updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId(jwt), request);
    }

    /** PUT /api/me/password - 204; afterwards every device has to log in again. */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(jwt), request);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/me/points - balance and history. */
    @GetMapping("/points")
    public PointsHistoryDto points(@AuthenticationPrincipal Jwt jwt) {
        return pointsService.getHistory(userId(jwt));
    }

    /** GET /api/me/warranties - active first, then expired. */
    @GetMapping("/warranties")
    public List<WarrantyDto> warranties(@AuthenticationPrincipal Jwt jwt) {
        return warrantyService.getMyWarranties(userId(jwt));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
