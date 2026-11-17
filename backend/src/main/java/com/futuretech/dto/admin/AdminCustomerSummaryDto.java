package com.futuretech.dto.admin;

import java.time.LocalDateTime;

import com.futuretech.entity.User;

/** One row of the admin customer list. No password hash, lock or token data. */
public record AdminCustomerSummaryDto(Long id, String email, String firstName, String lastName, String phone,
                                      int pointsBalance, LocalDateTime createdAt) {

    public static AdminCustomerSummaryDto from(User u) {
        return new AdminCustomerSummaryDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getPointsBalance(), u.getCreatedAt());
    }
}
