package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ImpairmentEntry;
import com.erp.finance.domain.model.ImpairmentEntryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpairmentEntryRepository extends JpaRepository<ImpairmentEntry, Long> {
  // 멱등 — 같은 자산·기간·유형(인식/환입) 이력 존재 여부.
  boolean existsByFixedAssetIdAndFiscalPeriodIdAndEntryType(
      Long fixedAssetId, Long fiscalPeriodId, ImpairmentEntryType entryType);

  List<ImpairmentEntry> findByFixedAssetIdOrderByFiscalPeriodIdAsc(Long fixedAssetId);
}
