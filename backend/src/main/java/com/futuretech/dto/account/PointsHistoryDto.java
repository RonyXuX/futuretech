package com.futuretech.dto.account;

import java.time.LocalDateTime;
import java.util.List;

import com.futuretech.entity.PointTransaction;
import com.futuretech.entity.enums.PointReason;

public record PointsHistoryDto(int balance, List<Entry> history) {

    public record Entry(int points, PointReason reason, Long orderId, LocalDateTime createdAt) {

        public static Entry from(PointTransaction t) {
            // getOrder().getId() doesn't load the order: Hibernate already knows the id
            return new Entry(t.getPoints(), t.getReason(), t.getOrder() == null ? null : t.getOrder().getId(),
                    t.getCreatedAt());
        }
    }
}
