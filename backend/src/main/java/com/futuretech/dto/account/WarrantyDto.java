package com.futuretech.dto.account;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.futuretech.entity.OrderItem;
import com.futuretech.entity.Warranty;

/** One row of "my warranties". Status and days left are calculated for the given day. */
public record WarrantyDto(
        Long warrantyId,
        Long orderId,
        Long productId,
        String sku,
        String productName,
        LocalDate startDate,
        LocalDate expiryDate,
        Status status,
        long daysLeft) {

    public enum Status { ACTIVE, EXPIRED }

    /** Must be called inside a transaction (order item, order and product are loaded lazily). */
    public static WarrantyDto from(Warranty w, LocalDate today) {
        OrderItem item = w.getOrderItem();
        boolean active = w.isActiveOn(today);
        long daysLeft = active ? ChronoUnit.DAYS.between(today, w.getExpiryDate()) : 0;
        return new WarrantyDto(w.getId(), item.getOrder().getId(), item.getProduct().getId(), item.getProduct().getSku(),
                item.getProduct().getName(), w.getStartDate(), w.getExpiryDate(),
                active ? Status.ACTIVE : Status.EXPIRED, daysLeft);
    }
}
