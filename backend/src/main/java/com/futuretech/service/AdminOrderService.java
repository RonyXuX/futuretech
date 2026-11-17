package com.futuretech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminOrderDto;
import com.futuretech.dto.admin.AdminOrderSummaryDto;
import com.futuretech.dto.admin.OrderCustomerDto;
import com.futuretech.entity.Order;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.OrderRepository;

/** Admin view of every customer's orders. The allowed status moves live in OrderStatus.canChangeTo. */
@Service
public class AdminOrderService {

    // id breaks ties between orders placed in the same instant, so paging is stable
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public AdminOrderService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /** status = null lists every order. */
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryDto> searchOrders(OrderStatus status, int page, int size) {
        Pageable pageable = Paging.of(page, size, NEWEST_FIRST);
        Page<Order> orders = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(orders, AdminOrderSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public AdminOrderDto getOrder(Long id) {
        return toDto(orderRepository.findDetailedById(id).orElseThrow(() -> orderNotFound(id)));
    }

    /** Cancelling goes through OrderService.cancel, so stock, warranties and points are undone too. */
    @Transactional
    public AdminOrderDto changeStatus(Long id, OrderStatus newStatus) {
        // row lock: two admins (or an admin and the customer) can't change the same order at once
        Order order = orderRepository.findWithLockById(id).orElseThrow(() -> orderNotFound(id));
        if (!order.getStatus().canChangeTo(newStatus)) {
            throw new ConflictException("An order can't go from " + order.getStatus() + " to " + newStatus + ".");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            orderService.cancel(order);
        } else {
            order.setStatus(newStatus);
        }
        return toDto(order);
    }

    private AdminOrderDto toDto(Order order) {
        return new AdminOrderDto(orderService.toDto(order), OrderCustomerDto.from(order.getUser()));
    }

    private static ResourceNotFoundException orderNotFound(Long id) {
        return new ResourceNotFoundException("Order " + id + " not found");
    }
}
