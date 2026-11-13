package com.futuretech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.futuretech.config.SecurityConfig;
import com.futuretech.dto.auth.AuthResponse;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.AuthException;
import com.futuretech.service.auth.AuthService;
import com.futuretech.support.TestSecurity;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String LOGIN_JSON = """
            {"email": "lina@example.com", "password": "Demo@2026"}""";

    @Test
    void login_returnsAccessTokenInBody_andRefreshTokenInASafeCookie() throws Exception {
        when(authService.login(any(), anyString())).thenReturn(result());

        MvcResult res = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())   // never in the body
                .andReturn();

        String cookie = res.getResponse().getHeader("Set-Cookie");
        assertThat(cookie)
                .startsWith("refresh_token=refresh-token")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth", "Max-Age=604800");  // 7 days
    }

    @Test
    void login_lockedAccount_returns423WithRetryAfter() throws Exception {
        when(authService.login(any(), anyString())).thenThrow(AuthException.accountLocked(Duration.ofMinutes(15)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isLocked())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void login_tooManyAttempts_returns429() throws Exception {
        when(authService.login(any(), anyString())).thenThrow(AuthException.tooManyAttempts(Duration.ofSeconds(30)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void register_weakPassword_returns400WithTheFieldMessage() throws Exception {
        String body = """
                {"email": "new@example.com", "password": "abcdefgh", "firstName": "Sam", "lastName": "Lee"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("must contain at least one letter and one digit"));
    }

    @Test
    void register_badEmail_returns400() throws Exception {
        String body = """
                {"email": "not-an-email", "password": "Secret123", "firstName": "Sam", "lastName": "Lee"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void register_success_returns201() throws Exception {
        when(authService.register(any(), anyString())).thenReturn(result());
        String body = """
                {"email": "new@example.com", "password": "Secret123", "firstName": "Sam", "lastName": "Lee"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_readsTheCookie() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(result());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
        verify(authService).refresh(eq("refresh-token"));
    }

    @Test
    void refresh_invalidCookie_returns401() throws Exception {
        when(authService.refresh(any())).thenThrow(AuthException.invalidRefreshToken());

        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_deletesTheCookie() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(authService).logout("refresh-token");
        assertThat(res.getResponse().getHeader("Set-Cookie")).contains("refresh_token=", "Max-Age=0");
    }

    private static AuthService.AuthResult result() {
        UserDto user = new UserDto(1L, "lina@example.com", "Lina", "Haddad", null, Role.CUSTOMER, 0);
        return new AuthService.AuthResult(AuthResponse.bearer("jwt-token", 900, user), "refresh-token");
    }
}
