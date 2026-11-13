package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.Warranty;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    // All warranties for one customer (walks warranty -> orderItem -> order -> user)
    List<Warranty> findByOrderItemOrderUserId(Long userId);
}
