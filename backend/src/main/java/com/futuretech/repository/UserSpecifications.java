package com.futuretech.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;

/** Search conditions for the admin customer list. */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /** Customers only (admins never appear), optionally whose email or full name contains the text. */
    public static Specification<User> customersMatching(String search) {
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("role"), Role.CUSTOMER);
        if (search == null || search.isBlank()) {
            return spec;
        }
        String pattern = "%" + ProductSpecifications.escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), pattern, '\\'),
                // "lina had" should find Lina Haddad, so search the full name, not each part
                cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                        pattern, '\\')));
    }
}
