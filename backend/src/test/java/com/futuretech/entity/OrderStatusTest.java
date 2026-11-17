package com.futuretech.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.futuretech.entity.enums.OrderStatus;

/** Every one of the 16 from -> to pairs, so no move can be allowed by accident. */
class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "PLACED,    PLACED,    false",
            "PLACED,    SHIPPED,   true",
            "PLACED,    DELIVERED, false",
            "PLACED,    CANCELLED, true",
            "SHIPPED,   PLACED,    false",
            "SHIPPED,   SHIPPED,   false",
            "SHIPPED,   DELIVERED, true",
            "SHIPPED,   CANCELLED, false",
            "DELIVERED, PLACED,    false",
            "DELIVERED, SHIPPED,   false",
            "DELIVERED, DELIVERED, false",
            "DELIVERED, CANCELLED, false",
            "CANCELLED, PLACED,    false",
            "CANCELLED, SHIPPED,   false",
            "CANCELLED, DELIVERED, false",
            "CANCELLED, CANCELLED, false"
    })
    void canChangeTo(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }
}
