package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Order history for one customer, newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
