package com.futuretech.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.order.OrderDto;
import com.futuretech.dto.order.OrderSummaryDto;
import com.futuretech.dto.order.PlaceOrderRequest;
import com.futuretech.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> placeOrder(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        OrderDto order = orderService.placeOrder(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping
    public List<OrderSummaryDto> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getMyOrders(userId(jwt));
    }

    @GetMapping("/{id}")
    public OrderDto myOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.getMyOrder(userId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.cancelMyOrder(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
