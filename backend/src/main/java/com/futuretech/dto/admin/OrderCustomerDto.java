package com.futuretech.dto.admin;

import com.futuretech.entity.User;

/** Who placed an order. Only contact details: no password hash, lock or token data. */
public record OrderCustomerDto(Long id, String email, String firstName, String lastName) {

    public static OrderCustomerDto from(User u) {
        return new OrderCustomerDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName());
    }
}
