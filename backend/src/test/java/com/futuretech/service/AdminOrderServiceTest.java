package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminOrderDto;
import com.futuretech.dto.admin.AdminOrderSummaryDto;
import com.futuretech.entity.Order;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;

    @InjectMocks
    private AdminOrderService service;

    // ---------- list ----------

    @Test
    void searchOrders_withoutStatus_listsEveryOrderNewestFirst() {
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page(order(OrderStatus.PLACED)));

        PageResponse<AdminOrderSummaryDto> result = service.searchOrders(null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().customer().email()).isEqualTo("lina.haddad@example.com");
        Pageable used = capturePageable();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchOrders_withStatus_filtersByIt() {
        when(orderRepository.findByStatus(any(), any())).thenReturn(page(order(OrderStatus.SHIPPED)));

        service.searchOrders(OrderStatus.SHIPPED, 0, 20);

        verify(orderRepository).findByStatus(any(), any());
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void searchOrders_badPageAndSize_areCorrected() {
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page());

        service.searchOrders(null, -3, 5000);

        Pageable used = capturePageable();
        assertThat(used.getPageNumber()).isZero();
        assertThat(used.getPageSize()).isEqualTo(Paging.MAX_PAGE_SIZE);
    }

    // ---------- detail ----------

    @Test
    void getOrder_includesTheCustomer() {
        when(orderRepository.findDetailedById(50L)).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));

        AdminOrderDto result = service.getOrder(50L);

        assertThat(result.customer().firstName()).isEqualTo("Lina");
    }

    @Test
    void getOrder_unknownId_throwsNotFound() {
        when(orderRepository.findDetailedById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- status changes ----------

    @Test
    void changeStatus_placedToShipped_onlyChangesTheStatus() {
        Order order = order(OrderStatus.PLACED);
        when(orderRepository.findWithLockById(50L)).thenReturn(Optional.of(order));

        service.changeStatus(50L, OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderService, never()).cancel(any());
    }

    @Test
    void changeStatus_shippedToDelivered_isAllowed() {
        Order order = order(OrderStatus.SHIPPED);
        when(orderRepository.findWithLockById(50L)).thenReturn(Optional.of(order));

        service.changeStatus(50L, OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void changeStatus_toCancelled_usesTheSameCancelAsCustomers() {
        Order order = order(OrderStatus.PLACED);
        when(orderRepository.findWithLockById(50L)).thenReturn(Optional.of(order));

        service.changeStatus(50L, OrderStatus.CANCELLED);

        // OrderService.cancel restores stock, removes warranties and reverses points (tested in OrderServiceTest)
        verify(orderService).cancel(order);
    }

    @Test
    void changeStatus_shippedOrderCantBeCancelled() {
        Order order = order(OrderStatus.SHIPPED);
        when(orderRepository.findWithLockById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.CANCELLED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SHIPPED to CANCELLED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderService, never()).cancel(any());
    }

    @Test
    void changeStatus_cantMoveBackwards() {
        Order order = order(OrderStatus.DELIVERED);
        when(orderRepository.findWithLockById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.SHIPPED))
                .isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void changeStatus_unknownId_throwsNotFound() {
        when(orderRepository.findWithLockById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(999L, OrderStatus.SHIPPED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- helpers ----------

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(pageable.capture());
        return pageable.getValue();
    }

    private static Page<Order> page(Order... orders) {
        return new PageImpl<>(List.of(orders));
    }

    private static Order order(OrderStatus status) {
        User user = new User();
        user.setId(5L);
        user.setEmail("lina.haddad@example.com");
        user.setFirstName("Lina");
        user.setLastName("Haddad");

        Order order = new Order();
        order.setId(50L);
        order.setUser(user);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("451.99"));
        return order;
    }
}
