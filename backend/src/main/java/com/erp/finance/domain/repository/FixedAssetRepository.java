package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.FixedAssetStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
  boolean existsByCode(String code);

  Page<FixedAsset> findByOrderByIdDesc(Pageable pageable);

  // 월 상각 처리 대상 — 가동(ACTIVE) 자산.
  List<FixedAsset> findByStatus(FixedAssetStatus status);
}
