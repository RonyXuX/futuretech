package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.PointTransaction;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
