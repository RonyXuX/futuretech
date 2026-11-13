package com.futuretech.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.account.PointsHistoryDto;
import com.futuretech.entity.Order;
import com.futuretech.entity.User;
import com.futuretech.entity.PointTransaction;
import com.futuretech.entity.enums.PointReason;
import com.futuretech.repository.PointTransactionRepository;
import com.futuretech.repository.UserRepository;

/**
 * The points rule lives here and only here: 1 point per whole dollar of the order total.
 * Every change is written to the ledger (point_transactions) AND added to the cached
 * balance on the user, in the caller's transaction.
 */
@Service
public class PointsService {

    private final PointTransactionRepository transactions;
    private final UserRepository userRepository;

    public PointsService(PointTransactionRepository transactions, UserRepository userRepository) {
        this.transactions = transactions;
        this.userRepository = userRepository;
    }

    /** 1535.99 -> 1535 points. */
    public int pointsFor(BigDecimal total) {
        return total.setScale(0, RoundingMode.FLOOR).intValueExact();
    }

    /** Called at checkout. Returns the points earned. */
    public int awardForOrder(Order order) {
        int points = pointsFor(order.getTotalAmount());
        record(order, points, PointReason.EARNED_PURCHASE);
        return points;
    }

    /** Called when an order is cancelled: takes back whatever the order is still worth. */
    public void reverseForOrder(Order order) {
        int remaining = transactions.sumForOrder(order.getId());
        if (remaining > 0) {
            record(order, -remaining, PointReason.ORDER_CANCELLED);
        }
    }

    /** Net points of one order (0 for a cancelled order). */
    public int pointsForOrder(Long orderId) {
        return transactions.sumForOrder(orderId);
    }

    /** GET /api/me/points: balance + every change, newest first. */
    @Transactional(readOnly = true)
    public PointsHistoryDto getHistory(Long userId) {
        int balance = userRepository.findById(userId).map(User::getPointsBalance).orElse(0);
        return new PointsHistoryDto(balance, transactions.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(PointsHistoryDto.Entry::from)
                .toList());
    }

    private void record(Order order, int points, PointReason reason) {
        PointTransaction t = new PointTransaction();
        t.setUser(order.getUser());
        t.setOrder(order);
        t.setPoints(points);
        t.setReason(reason);
        transactions.save(t);
        userRepository.addPoints(order.getUser().getId(), points);
    }
}
