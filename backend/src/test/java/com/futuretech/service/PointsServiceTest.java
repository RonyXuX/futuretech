package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futuretech.entity.Order;
import com.futuretech.entity.PointTransaction;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.PointReason;
import com.futuretech.repository.PointTransactionRepository;
import com.futuretech.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock private PointTransactionRepository transactions;
    @Mock private UserRepository userRepository;
    @InjectMocks private PointsService pointsService;

    @Test
    void onePointPerWholeDollar_roundedDown() {
        assertThat(pointsService.pointsFor(new BigDecimal("1535.99"))).isEqualTo(1535);
        assertThat(pointsService.pointsFor(new BigDecimal("8.99"))).isEqualTo(8);
        assertThat(pointsService.pointsFor(new BigDecimal("0.50"))).isZero();
    }

    @Test
    void awardForOrder_writesLedgerAndUpdatesBalance() {
        Order order = order("611.97");

        int points = pointsService.awardForOrder(order);

        assertThat(points).isEqualTo(611);
        ArgumentCaptor<PointTransaction> saved = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactions).save(saved.capture());
        assertThat(saved.getValue().getPoints()).isEqualTo(611);
        assertThat(saved.getValue().getReason()).isEqualTo(PointReason.EARNED_PURCHASE);
        verify(userRepository).addPoints(5L, 611);
    }

    @Test
    void reverseForOrder_takesBackWhatTheOrderIsStillWorth() {
        Order order = order("611.97");
        when(transactions.sumForOrder(50L)).thenReturn(611);

        pointsService.reverseForOrder(order);

        verify(userRepository).addPoints(5L, -611);
    }

    @Test
    void reverseForOrder_nothingLeft_doesNothing() {
        when(transactions.sumForOrder(50L)).thenReturn(0);

        pointsService.reverseForOrder(order("10.00"));

        verify(transactions, never()).save(any());
    }

    private static Order order(String total) {
        User user = new User();
        user.setId(5L);
        Order order = new Order();
        order.setId(50L);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal(total));
        return order;
    }
}
