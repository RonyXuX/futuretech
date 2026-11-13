package com.futuretech.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.futuretech.config.SecurityConfig;
import com.futuretech.dto.PageResponse;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;
import com.futuretech.service.ProductService;
import com.futuretech.service.UserService;
import com.futuretech.service.auth.JwtService;
import com.futuretech.support.TestSecurity;

/**
 * Checks the access rules in SecurityConfig with real HTTP requests: what is public, what
 * needs a login, what needs the ADMIN role, and that bad tokens are refused.
 */
@WebMvcTest({MeController.class, ProductController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;  // the app's real encoder, so tokens are signed exactly like in production

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProductService productService;

    @Test
    void browsingProducts_needsNoLogin() throws Exception {
        when(productService.searchProducts(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new PageResponse<>(java.util.List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_withRealSignedToken_returnsTheUserFromTheToken() throws Exception {
        when(userService.getUser(5L)).thenReturn(
                new UserDto(5L, "lina@example.com", "Lina", "Haddad", null, Role.CUSTOMER, 120));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(5L, Role.CUSTOMER, Clock.systemUTC())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lina@example.com"))
                .andExpect(jsonPath("$.pointsBalance").value(120));
    }

    @Test
    void me_withExpiredToken_returns401() throws Exception {
        // a token issued 1 hour ago, valid 15 minutes -> expired 45 minutes ago
        Clock anHourAgo = Clock.offset(Clock.systemUTC(), Duration.ofHours(-1));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(5L, Role.CUSTOMER, anHourAgo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        String token = tokenFor(5L, Role.CUSTOMER, Clock.systemUTC());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";  // breaks the signature

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminArea_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminArea_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminArea_asAdmin_isLetThrough() throws Exception {
        // no admin controller exists yet, so getting past security means a 404, not a 401/403
        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + tokenFor(1L, Role.ADMIN, Clock.systemUTC())))
                .andExpect(status().isNotFound());
    }

    /** Signs a token the same way the app does, with the given clock as "now". */
    private String tokenFor(Long userId, Role role, Clock clock) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user" + userId + "@example.com");
        user.setRole(role);
        return new JwtService(jwtEncoder, TestSecurity.properties(), clock).createAccessToken(user);
    }
}
