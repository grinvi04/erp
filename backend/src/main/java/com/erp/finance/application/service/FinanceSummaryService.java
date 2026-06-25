package com.erp.finance.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.FinanceSummaryResponse;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceSummaryService {

    private final ApInvoiceRepository apInvoiceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PermissionChecker permissionChecker;

    public FinanceSummaryResponse getSummary() {
        permissionChecker.require(Permission.FINANCE_READ);
        BigDecimal unpaidAmount = apInvoiceRepository.sumUnpaidAmount();
        return new FinanceSummaryResponse(
                apInvoiceRepository.countUnpaid(),
                unpaidAmount != null ? unpaidAmount : BigDecimal.ZERO,
                journalEntryRepository.countByStatus(JournalEntryStatus.DRAFT));
    }
}
