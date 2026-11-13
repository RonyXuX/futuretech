package com.futuretech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.SavedBuild;

public interface SavedBuildRepository extends JpaRepository<SavedBuild, Long> {

    // A customer's saved builds, newest first
    List<SavedBuild> findByUserIdOrderByCreatedAtDesc(Long userId);
}
