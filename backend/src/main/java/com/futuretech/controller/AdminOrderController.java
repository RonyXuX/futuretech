package com.futuretech.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminOrderDto;
import com.futuretech.dto.admin.AdminOrderSummaryDto;
import com.futuretech.dto.admin.OrderStatusRequest;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.service.AdminOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public PageResponse<AdminOrderSummaryDto> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderService.searchOrders(status, page, size);
    }

    @GetMapping("/{id}")
    public AdminOrderDto getOrder(@PathVariable Long id) {
        return adminOrderService.getOrder(id);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderDto changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return adminOrderService.changeStatus(id, request.status());
    }
}
