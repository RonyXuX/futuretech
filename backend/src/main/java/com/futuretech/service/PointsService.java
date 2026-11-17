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
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.repository.PointTransactionRepository;
import com.futuretech.repository.UserRepository;

/** The only place with the points rule: 1 point per whole dollar. Writes ledger + cached balance. */
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

    public int awardForOrder(Order order) {
        int points = pointsFor(order.getTotalAmount());
        record(order, points, PointReason.EARNED_PURCHASE);
        return points;
    }

    /** Takes back what the order is still worth (net, so admin adjustments are respected). */
    public void reverseForOrder(Order order) {
        int remaining = transactions.sumForOrder(order.getId());
        if (remaining > 0) {
            record(order, -remaining, PointReason.ORDER_CANCELLED);
        }
    }

    /**
     * Admin correction, not linked to an order (amount may be negative). Refused if the balance
     * would go below 0; the database checks that in the same UPDATE, so it can't race.
     */
    public void adjustByAdmin(User user, int amount) {
        if (amount == 0) {
            throw new InvalidInputException("amount", "must not be 0");
        }
        int balanceBefore = user.getPointsBalance();
        if (userRepository.addPointsIfNotNegative(user.getId(), amount) == 0) {
            throw new ConflictException("The balance is " + balanceBefore + " points, so " + amount
                    + " would make it negative.");
        }
        PointTransaction t = new PointTransaction();
        t.setUser(user);
        t.setPoints(amount);
        t.setReason(PointReason.ADMIN_ADJUSTMENT);
        transactions.save(t);
    }

    public int pointsForOrder(Long orderId) {
        return transactions.sumForOrder(orderId);
    }

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
