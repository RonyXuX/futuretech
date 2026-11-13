package com.futuretech.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.futuretech.entity.Order;
import com.futuretech.entity.OrderItem;
import com.futuretech.entity.Warranty;
import com.futuretech.entity.enums.OrderStatus;

/** A full order: lines, shipping, points earned and each line's warranty end date. */
public record OrderDto(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        int pointsEarned,
        LocalDateTime createdAt,
        PlaceOrderRequest.Shipping shipping,
        List<Line> items) {

    public record Line(Long productId, String sku, String name, int quantity, BigDecimal unitPrice,
                       BigDecimal lineTotal, LocalDate warrantyExpiresOn) {
    }

    /**
     * @param warrantiesByItemId warranty of each order line (missing for cancelled orders)
     * Must be called inside a transaction (items and products are loaded lazily).
     */
    public static OrderDto from(Order o, int pointsEarned, Map<Long, Warranty> warrantiesByItemId) {
        List<Line> lines = o.getItems().stream().map(i -> line(i, warrantiesByItemId.get(i.getId()))).toList();
        PlaceOrderRequest.Shipping shipping = new PlaceOrderRequest.Shipping(
                o.getShippingFullName(), o.getShippingAddress(), o.getShippingCity(), o.getShippingPhone());
        return new OrderDto(o.getId(), o.getStatus(), o.getTotalAmount(), pointsEarned, o.getCreatedAt(), shipping, lines);
    }

    private static Line line(OrderItem i, Warranty warranty) {
        return new Line(i.getProduct().getId(), i.getProduct().getSku(), i.getProduct().getName(), i.getQuantity(),
                i.getUnitPrice(), i.getLineTotal(), warranty == null ? null : warranty.getExpiryDate());
    }
}
