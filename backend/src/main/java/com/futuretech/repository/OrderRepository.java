package com.futuretech.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.futuretech.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Someone else's order looks exactly like a missing one, so ids can't be probed
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Row lock for cancel, so two clicks can't both pass the "still PLACED?" check.
     * No EntityGraph: PostgreSQL can't lock rows of an outer join.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByIdAndUserId(Long id, Long userId);
}
