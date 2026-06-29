package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.DepreciationEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepreciationEntryRepository extends JpaRepository<DepreciationEntry, Long> {
  // 멱등 — 같은 자산·기간 상각 이력 존재 여부.
  boolean existsByFixedAssetIdAndFiscalPeriodId(Long fixedAssetId, Long fiscalPeriodId);

  List<DepreciationEntry> findByFixedAssetIdOrderByFiscalPeriodIdAsc(Long fixedAssetId);
}
