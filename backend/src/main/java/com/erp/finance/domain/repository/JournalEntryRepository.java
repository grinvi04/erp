package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    boolean existsByEntryNo(String entryNo);
    Page<JournalEntry> findByFiscalPeriodId(Long fiscalPeriodId, Pageable pageable);
    Page<JournalEntry> findByStatus(JournalEntryStatus status, Pageable pageable);
    long countByStatus(JournalEntryStatus status);
    Page<JournalEntry> findByFiscalPeriodIdAndStatus(Long fiscalPeriodId, JournalEntryStatus status, Pageable pageable);
    Optional<JournalEntry> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
