package com.futuretech.entity.enums;

/** Why a point transaction happened. */
public enum PointReason {
    EARNED_PURCHASE,
    ORDER_CANCELLED,   // the customer cancelled: the order's points are taken back
    ADMIN_ADJUSTMENT
}
