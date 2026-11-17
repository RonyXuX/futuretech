package com.futuretech.dto.admin;

import com.futuretech.entity.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}
