package com.futuretech.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.futuretech.dto.order.OrderDto;
import com.futuretech.dto.order.PlaceOrderRequest;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.service.OrderService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static final String VALID_ORDER = """
            {"items": [{"productId": 1, "quantity": 2}],
             "shipping": {"fullName": "Lina Haddad", "address": "12 Hamra Street", "city": "Beirut", "phone": "+961 70 000 001"}}""";

    @Test
    void placeOrder_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_usesTheUserIdFromTheToken_andReturns201() throws Exception {
        when(orderService.placeOrder(eq(5L), any())).thenReturn(order(77L));

        mockMvc.perform(post("/api/orders").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/77"))
                .andExpect(jsonPath("$.totalAmount").value(903.98));
        verify(orderService).placeOrder(eq(5L), any(PlaceOrderRequest.class));
    }

    @Test
    void placeOrder_quantityZero_returns400WithFieldName() throws Exception {
        String body = VALID_ORDER.replace("\"quantity\": 2", "\"quantity\": 0");

        mockMvc.perform(post("/api/orders").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void placeOrder_emptyCart_returns400() throws Exception {
        String body = """
                {"items": [], "shipping": {"fullName": "L", "address": "A", "city": "B", "phone": "1"}}""";

        mockMvc.perform(post("/api/orders").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void placeOrder_notEnoughStock_returns409WithEveryProblem() throws Exception {
        when(orderService.placeOrder(eq(5L), any())).thenThrow(new ConflictException(
                "Some items in your cart can't be ordered.", List.of("Logitech G502 X: only 2 left in stock (you asked for 3).")));

        mockMvc.perform(post("/api/orders").with(jwt().jwt(j -> j.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.problems[0]").value("Logitech G502 X: only 2 left in stock (you asked for 3)."));
    }

    @Test
    void getOrder_notMine_returns404() throws Exception {
        when(orderService.getMyOrder(5L, 9L)).thenThrow(new ResourceNotFoundException("Order 9 not found"));

        mockMvc.perform(get("/api/orders/9").with(jwt().jwt(j -> j.subject("5"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shippedOrder_returns409() throws Exception {
        when(orderService.cancelMyOrder(5L, 9L)).thenThrow(new ConflictException("Only orders that have not shipped yet can be cancelled"));

        mockMvc.perform(post("/api/orders/9/cancel").with(jwt().jwt(j -> j.subject("5"))))
                .andExpect(status().isConflict());
    }

    private static OrderDto order(Long id) {
        return new OrderDto(id, OrderStatus.PLACED, new BigDecimal("903.98"), 903, LocalDateTime.now(),
                new PlaceOrderRequest.Shipping("Lina Haddad", "12 Hamra Street", "Beirut", "+961"), List.of());
    }
}
