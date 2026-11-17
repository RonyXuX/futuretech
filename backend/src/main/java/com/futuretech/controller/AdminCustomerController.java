package com.futuretech.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminCustomerDto;
import com.futuretech.dto.admin.AdminCustomerSummaryDto;
import com.futuretech.dto.admin.PointsAdjustmentRequest;
import com.futuretech.service.AdminCustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    /** search matches the email or the full name, ignoring case. */
    @GetMapping
    public PageResponse<AdminCustomerSummaryDto> searchCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminCustomerService.searchCustomers(search, page, size);
    }

    @GetMapping("/{id}")
    public AdminCustomerDto getCustomer(@PathVariable Long id) {
        return adminCustomerService.getCustomer(id);
    }

    // POST, not PUT: each call adds a new ledger entry instead of setting a value
    @PostMapping("/{id}/points")
    public AdminCustomerDto adjustPoints(@PathVariable Long id, @Valid @RequestBody PointsAdjustmentRequest request) {
        return adminCustomerService.adjustPoints(id, request.amount());
    }
}
