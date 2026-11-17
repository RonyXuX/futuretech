package com.futuretech.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.futuretech.entity.Order;
import com.futuretech.entity.enums.OrderStatus;

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

    // ---------- admin: every customer's orders ----------

    // Only the customer is joined: fetching the items collection here would make Hibernate
    // page in memory. The items come in one batch per page instead (@BatchSize on Order.items).
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<Order> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(Long id);
}
