package com.futuretech.repository.spec;

import org.springframework.data.jpa.repository.JpaRepository;

import com.futuretech.entity.spec.CoolerSpec;

public interface CoolerSpecRepository extends JpaRepository<CoolerSpec, Long> {
}
