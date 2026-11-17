package com.futuretech.entity.enums;

public enum OrderStatus {
    PLACED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * The only allowed moves: PLACED -> SHIPPED -> DELIVERED, or PLACED -> CANCELLED.
     * Nothing moves backwards, and a shipped order can no longer be cancelled.
     */
    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
