package com.futuretech.dto.admin;

import com.futuretech.dto.order.OrderDto;

/** The same order details customers see, plus who ordered. */
public record AdminOrderDto(OrderDto order, OrderCustomerDto customer) {
}
