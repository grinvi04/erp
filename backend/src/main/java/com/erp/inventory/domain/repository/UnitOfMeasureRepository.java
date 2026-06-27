package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
  boolean existsByCode(String code);
}
