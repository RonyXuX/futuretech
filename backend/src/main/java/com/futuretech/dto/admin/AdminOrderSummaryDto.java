package com.futuretech.dto.admin;

import com.futuretech.dto.order.OrderSummaryDto;
import com.futuretech.entity.Order;

/** One row of the admin order list: the same summary customers see, plus who ordered. */
public record AdminOrderSummaryDto(OrderSummaryDto order, OrderCustomerDto customer) {

    /** Needs an open transaction: the items are lazy. */
    public static AdminOrderSummaryDto from(Order o) {
        return new AdminOrderSummaryDto(OrderSummaryDto.from(o), OrderCustomerDto.from(o.getUser()));
    }
}
