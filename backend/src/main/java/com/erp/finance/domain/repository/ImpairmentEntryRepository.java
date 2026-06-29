package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ImpairmentEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpairmentEntryRepository extends JpaRepository<ImpairmentEntry, Long> {
  // 멱등 — 같은 자산·기간 손상 인식 이력 존재 여부.
  boolean existsByFixedAssetIdAndFiscalPeriodId(Long fixedAssetId, Long fiscalPeriodId);

  List<ImpairmentEntry> findByFixedAssetIdOrderByFiscalPeriodIdAsc(Long fixedAssetId);
}
