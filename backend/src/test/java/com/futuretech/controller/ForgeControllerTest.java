package com.futuretech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.futuretech.config.SecurityConfig;
import com.futuretech.dto.forge.BuildCheckRequest;
import com.futuretech.dto.forge.CompatibilityReport;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.forge.Issue;
import com.futuretech.service.CompatibilityService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(ForgeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class ForgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompatibilityService compatibilityService;

    @Test
    void guestsCanCheck_withoutAToken() throws Exception {
        when(compatibilityService.check(any())).thenReturn(report());

        mockMvc.perform(post("/api/forge/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 1}, {\"productId\": 7, \"quantity\": 1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.errors[0].rule").value("SOCKET"))
                .andExpect(jsonPath("$.errors[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].productIds[1]").value(7))
                .andExpect(jsonPath("$.missingParts[0]").value("RAM"))
                .andExpect(jsonPath("$.estimatedWattage").value(470));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BuildCheckRequest.Item>> items = ArgumentCaptor.forClass(List.class);
        verify(compatibilityService).check(items.capture());
        assertThat(items.getValue()).extracting(BuildCheckRequest.Item::productId).containsExactly(1L, 7L);
    }

    @Test
    void emptyBuild_isAllowed() throws Exception {
        when(compatibilityService.check(any())).thenReturn(report());

        mockMvc.perform(post("/api/forge/check").contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
                .andExpect(status().isOk());
    }

    @Test
    void missingItems_returns400() throws Exception {
        mockMvc.perform(post("/api/forge/check").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
        verifyNoInteractions(compatibilityService);
    }

    @Test
    void quantityAbove8_returns400() throws Exception {
        mockMvc.perform(post("/api/forge/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 9}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
        verifyNoInteractions(compatibilityService);
    }

    @Test
    void moreThan30Products_returns400() throws Exception {
        String items = IntStream.rangeClosed(1, 31)
                .mapToObj(i -> "{\"productId\": " + i + ", \"quantity\": 1}")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/forge/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [" + items + "]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(compatibilityService);
    }

    @Test
    void otherForgeUrls_stillNeedALogin() throws Exception {
        // only POST /check is public; nothing else under /api/forge is
        mockMvc.perform(post("/api/forge/other").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private static CompatibilityReport report() {
        return new CompatibilityReport(false, false,
                List.of(Issue.error("SOCKET", "CPU needs AM5, board is LGA1851.", 1L, 7L)),
                List.of(), List.of(ProductType.RAM), 470, new BigDecimal("699.98"));
    }
}
