package com.futuretech.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.futuretech.entity.Order;
import com.futuretech.entity.enums.OrderStatus;

/** One row of the order history list. */
public record OrderSummaryDto(Long id, OrderStatus status, BigDecimal totalAmount, int itemCount,
                              LocalDateTime createdAt) {

    public static OrderSummaryDto from(Order o) {
        int itemCount = o.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
        return new OrderSummaryDto(o.getId(), o.getStatus(), o.getTotalAmount(), itemCount, o.getCreatedAt());
    }
}
