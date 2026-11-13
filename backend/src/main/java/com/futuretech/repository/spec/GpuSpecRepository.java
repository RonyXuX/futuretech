package com.futuretech.repository.spec;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.spec.GpuSpec;

public interface GpuSpecRepository extends JpaRepository<GpuSpec, Long> {
}
