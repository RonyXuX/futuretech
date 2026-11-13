package com.futuretech.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.auth.UserDto;
import com.futuretech.service.UserService;

/**
 * Endpoints about the logged-in user. The user id comes from the verified JWT ("sub" claim),
 * never from the URL, so nobody can read another person's data by changing an id.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/me - who am I? Needs "Authorization: Bearer <accessToken>". */
    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(Long.valueOf(jwt.getSubject()));
    }
}
