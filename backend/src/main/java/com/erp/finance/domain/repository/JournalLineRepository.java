package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByJournalEntryIdOrderByLineNoAsc(Long journalEntryId);
}
