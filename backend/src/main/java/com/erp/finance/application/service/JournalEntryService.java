package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.dto.JournalLineResponse;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalLine;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long ENTRY_NO_MODULUS = 100000L;
    private static final int ENTRY_NO_MAX_RETRIES = 10;

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionChecker permissionChecker;

    public JournalEntryResponse findById(Long id) {
        permissionChecker.require(Permission.FINANCE_READ);
        return JournalEntryResponse.from(getOrThrow(id));
    }

    public PageResponse<JournalEntryResponse> findByFiscalPeriod(Long fiscalPeriodId, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        return PageResponse.from(
            journalEntryRepository.findByFiscalPeriodId(fiscalPeriodId, pageable)
                .map(JournalEntryResponse::from));
    }

    public List<JournalLineResponse> findLines(Long journalEntryId) {
        permissionChecker.require(Permission.FINANCE_READ);
        getOrThrow(journalEntryId);
        return journalLineRepository.findByJournalEntryIdOrderByLineNoAsc(journalEntryId).stream()
            .map(JournalLineResponse::from)
            .toList();
    }

    @Transactional
    public JournalEntryResponse create(JournalEntryCreateRequest request) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        FiscalPeriod period = fiscalPeriodRepository.findById(request.fiscalPeriodId())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        if (!period.isOpen()) {
            throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
        }
        if (request.entryDate().isBefore(period.getStartDate()) || request.entryDate().isAfter(period.getEndDate())) {
            throw new ErpException(ErrorCode.JOURNAL_ENTRY_DATE_OUT_OF_RANGE);
        }

        String entryNo = generateEntryNo(request.entryDate());
        JournalEntry entry = JournalEntry.create(entryNo, request.entryDate(), period,
            request.description(), request.entryType(), request.currency());

        int lineNo = 1;
        for (JournalLineRequest lineReq : request.lines()) {
            Account account = accountRepository.findById(lineReq.accountId())
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
            account.assertPostable();
            JournalLine line = JournalLine.of(entry, lineNo++, account,
                lineReq.debitAmount(), lineReq.creditAmount(), lineReq.description(), lineReq.departmentId());
            entry.addLine(line);
        }

        if (!entry.isBalanced()) {
            throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
        }

        return JournalEntryResponse.from(journalEntryRepository.save(entry));
    }

    @Transactional
    public JournalEntryResponse post(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        JournalEntry entry = getOrThrow(id);
        String userId = currentUserProvider.getCurrentUserId();
        entry.post(userId != null ? userId : "SYSTEM");
        return JournalEntryResponse.from(entry);
    }

    private JournalEntry getOrThrow(Long id) {
        return journalEntryRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_FOUND));
    }

    private String generateEntryNo(LocalDate date) {
        String base = "JE-" + date.format(DATE_FMT) + "-";
        for (int i = 0; i < ENTRY_NO_MAX_RETRIES; i++) {
            String entryNo = base + String.format("%05d", System.currentTimeMillis() % ENTRY_NO_MODULUS);
            if (!journalEntryRepository.existsByEntryNo(entryNo)) {
                return entryNo;
            }
        }
        throw new ErpException(ErrorCode.INTERNAL_ERROR);
    }
}
