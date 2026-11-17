package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.account.PointsHistoryDto;
import com.futuretech.dto.admin.AdminCustomerDto;
import com.futuretech.dto.admin.AdminCustomerSummaryDto;
import com.futuretech.dto.order.OrderSummaryDto;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.entity.enums.PointReason;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminCustomerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderService orderService;
    @Mock private PointsService pointsService;

    @InjectMocks
    private AdminCustomerService service;

    @Test
    @SuppressWarnings("unchecked")
    void searchCustomers_newestFirst() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(5L, Role.CUSTOMER, 120))));

        PageResponse<AdminCustomerSummaryDto> result = service.searchCustomers("lina", 0, 20);

        assertThat(result.content().getFirst().email()).isEqualTo("lina.haddad@example.com");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getCustomer_includesOrdersAndPointsHistory() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.CUSTOMER, 451)));
        when(orderService.getMyOrders(5L)).thenReturn(List.of(
                new OrderSummaryDto(50L, OrderStatus.DELIVERED, new BigDecimal("451.99"), 1, LocalDateTime.now())));
        when(pointsService.getHistory(5L)).thenReturn(new PointsHistoryDto(451, List.of(
                new PointsHistoryDto.Entry(451, PointReason.EARNED_PURCHASE, 50L, LocalDateTime.now()))));

        AdminCustomerDto result = service.getCustomer(5L);

        assertThat(result.pointsBalance()).isEqualTo(451);
        assertThat(result.orders()).hasSize(1);
        assertThat(result.pointsHistory()).hasSize(1);
    }

    @Test
    void getCustomer_adminAccount_looksLikeItDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN, 0)));

        assertThatThrownBy(() -> service.getCustomer(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer 1 not found");
    }

    @Test
    void getCustomer_unknownId_throwsNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCustomer(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adjustPoints_goesThroughPointsServiceAndReloadsTheCustomer() {
        User before = user(5L, Role.CUSTOMER, 100);
        User after = user(5L, Role.CUSTOMER, 150);
        when(userRepository.findById(5L)).thenReturn(Optional.of(before)).thenReturn(Optional.of(after));
        when(pointsService.getHistory(5L)).thenReturn(new PointsHistoryDto(150, List.of()));

        AdminCustomerDto result = service.adjustPoints(5L, 50);

        verify(pointsService).adjustByAdmin(before, 50);
        verify(userRepository, times(2)).findById(5L);
        assertThat(result.pointsBalance()).isEqualTo(150);  // the fresh value, not the one loaded first
    }

    @Test
    void adjustPoints_refusedByPointsService_passesTheConflictOn() {
        User customer = user(5L, Role.CUSTOMER, 10);
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));
        doThrow(new ConflictException("would make it negative")).when(pointsService).adjustByAdmin(customer, -50);

        assertThatThrownBy(() -> service.adjustPoints(5L, -50)).isInstanceOf(ConflictException.class);
        verify(pointsService, never()).getHistory(any());
    }

    @Test
    void adjustPoints_adminAccount_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN, 0)));

        assertThatThrownBy(() -> service.adjustPoints(1L, 50)).isInstanceOf(ResourceNotFoundException.class);
        verify(pointsService, never()).adjustByAdmin(any(), anyInt());
    }

    private static User user(Long id, Role role, int points) {
        User user = new User();
        user.setId(id);
        user.setEmail("lina.haddad@example.com");
        user.setFirstName("Lina");
        user.setLastName("Haddad");
        user.setRole(role);
        user.setPointsBalance(points);
        return user;
    }
}
