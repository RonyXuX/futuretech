package com.futuretech.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminCustomerDto;
import com.futuretech.dto.admin.AdminCustomerSummaryDto;
import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.UserRepository;
import com.futuretech.repository.UserSpecifications;

/** Admin view of customer accounts. Read-only except for points; customers are never disabled or deleted. */
@Service
public class AdminCustomerService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PointsService pointsService;

    public AdminCustomerService(UserRepository userRepository, OrderService orderService,
                                PointsService pointsService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.pointsService = pointsService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCustomerSummaryDto> searchCustomers(String search, int page, int size) {
        return PageResponse.from(
                userRepository.findAll(UserSpecifications.customersMatching(search), Paging.of(page, size, NEWEST_FIRST)),
                AdminCustomerSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public AdminCustomerDto getCustomer(Long id) {
        return toDto(findCustomer(id));
    }

    /** One transaction: the ledger entry and the balance change are saved together or not at all. */
    @Transactional
    public AdminCustomerDto adjustPoints(Long id, int amount) {
        pointsService.adjustByAdmin(findCustomer(id), amount);
        // load again: the balance UPDATE ran in the database, so the User loaded above is out of date
        return toDto(findCustomer(id));
    }

    private AdminCustomerDto toDto(User customer) {
        return AdminCustomerDto.from(customer,
                orderService.getMyOrders(customer.getId()),
                pointsService.getHistory(customer.getId()).history());
    }

    // An admin account is not a customer, so it gives 404 here like a missing id
    private User findCustomer(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
    }
}
