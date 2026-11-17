package com.futuretech.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

import com.futuretech.dto.account.PointsHistoryDto;
import com.futuretech.dto.order.OrderSummaryDto;
import com.futuretech.entity.User;

/** One customer for the admin: profile plus the same orders and points history the customer sees. */
public record AdminCustomerDto(Long id, String email, String firstName, String lastName, String phone,
                               int pointsBalance, LocalDateTime createdAt,
                               List<OrderSummaryDto> orders, List<PointsHistoryDto.Entry> pointsHistory) {

    public static AdminCustomerDto from(User u, List<OrderSummaryDto> orders, List<PointsHistoryDto.Entry> history) {
        return new AdminCustomerDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getPointsBalance(), u.getCreatedAt(), orders, history);
    }
}
