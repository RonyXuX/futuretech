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

/** The user id always comes from the verified JWT, never from the URL, so ids can't be swapped. */
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

    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(userId(jwt));
    }

    @PutMapping
    public UserDto updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId(jwt), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(jwt), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/points")
    public PointsHistoryDto points(@AuthenticationPrincipal Jwt jwt) {
        return pointsService.getHistory(userId(jwt));
    }

    @GetMapping("/warranties")
    public List<WarrantyDto> warranties(@AuthenticationPrincipal Jwt jwt) {
        return warrantyService.getMyWarranties(userId(jwt));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
