package com.futuretech.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.futuretech.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Adds (or removes) points in ONE SQL statement: "balance = balance + delta". Reading the
     * balance in Java and writing it back could lose points if two orders finish at once.
     */
    @Modifying
    @Query("UPDATE User u SET u.pointsBalance = u.pointsBalance + :delta WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("delta") int delta);
}
