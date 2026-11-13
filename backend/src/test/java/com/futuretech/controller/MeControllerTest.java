package com.futuretech.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.futuretech.config.SecurityConfig;
import com.futuretech.dto.account.PointsHistoryDto;
import com.futuretech.dto.account.WarrantyDto;
import com.futuretech.dto.auth.ChangePasswordRequest;
import com.futuretech.dto.auth.UserDto;
import com.futuretech.entity.enums.PointReason;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.service.PointsService;
import com.futuretech.service.UserService;
import com.futuretech.service.WarrantyService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class MeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
    @MockitoBean private PointsService pointsService;
    @MockitoBean private WarrantyService warrantyService;

    @Test
    void updateProfile_returnsTheUpdatedUser() throws Exception {
        when(userService.updateProfile(eq(5L), any()))
                .thenReturn(new UserDto(5L, "lina@example.com", "Lina", "Khoury", null, Role.CUSTOMER, 10));

        mockMvc.perform(put("/api/me").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Lina", "lastName": "Khoury"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Khoury"));
    }

    @Test
    void updateProfile_blankName_returns400() throws Exception {
        mockMvc.perform(put("/api/me").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": " ", "lastName": "Khoury"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    void changePassword_success_returns204() throws Exception {
        mockMvc.perform(put("/api/me/password").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "OldPass123", "newPassword": "NewPass123"}"""))
                .andExpect(status().isNoContent());
        verify(userService).changePassword(5L, new ChangePasswordRequest("OldPass123", "NewPass123"));
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/me/password").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "OldPass123", "newPassword": "short1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("must be between 8 and 72 characters"));
    }

    @Test
    void changePassword_wrongCurrentPassword_returns400OnThatField() throws Exception {
        doThrow(new InvalidInputException("currentPassword", "is incorrect"))
                .when(userService).changePassword(eq(5L), any());

        mockMvc.perform(put("/api/me/password").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "guess", "newPassword": "NewPass123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").value("is incorrect"));
    }

    @Test
    void points_returnsBalanceAndHistory() throws Exception {
        when(pointsService.getHistory(5L)).thenReturn(new PointsHistoryDto(1634, List.of(
                new PointsHistoryDto.Entry(1635, PointReason.EARNED_PURCHASE, 2L, LocalDateTime.now()))));

        mockMvc.perform(get("/api/me/points").with(jwt().jwt(j -> j.subject("5"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1634))
                .andExpect(jsonPath("$.history[0].reason").value("EARNED_PURCHASE"));
    }

    @Test
    void warranties_returnsStatus() throws Exception {
        when(warrantyService.getMyWarranties(5L)).thenReturn(List.of(new WarrantyDto(1L, 2L, 3L, "FT-DESK-003",
                "FutureTech Forge Pro", LocalDate.of(2026, 3, 2), LocalDate.of(2028, 3, 2), WarrantyDto.Status.ACTIVE, 475)));

        mockMvc.perform(get("/api/me/warranties").with(jwt().jwt(j -> j.subject("5"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].daysLeft").value(475));
    }

    @Test
    void warranties_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/me/warranties")).andExpect(status().isUnauthorized());
    }
}
