package com.futuretech.dto.auth;

import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;

public record UserDto(Long id, String email, String firstName, String lastName, String phone,
                      Role role, Integer pointsBalance) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getRole(), u.getPointsBalance());
    }
}
