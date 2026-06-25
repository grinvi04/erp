package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceLine;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AP 전표 승인 → GL 자동 분개(DRAFT). 실무 표준 분개:
 * <pre>(차) 비용/자산·부가세대급금 [라인별 계정]  (대) 외상매입금 [공급업체 통제계정] 총액</pre>
 * 대변 외상매입금은 전표가 아니라 공급업체 마스터의 통제계정에서 온다(AP 보조원장 ↔ GL 일치).
 * 전기(POST)는 회계담당이 별도 수행한다(여기선 DRAFT만 생성).
 *
 * <p>AP·GL은 같은 finance 모듈이라 직접 호출(모듈 간 이벤트 불필요).
 */
@Service
@RequiredArgsConstructor
public class ApInvoicePostingService {

    private final JournalEntryService journalEntryService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;

    /**
     * 승인된 전표의 DRAFT 분개를 생성하고 분개 ID를 반환한다. 라인이 없거나 공급업체에
     * 외상매입금 통제계정이 설정되지 않았으면 전기하지 않고 {@code null}을 반환한다.
     */
    @Transactional
    public Long postDraft(ApInvoice invoice) {
        Account payables = invoice.getVendor().getPayablesAccount();
        if (!invoice.hasLines() || payables == null) {
            return null;
        }
        FiscalPeriod period = fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(invoice.getInvoiceDate(), invoice.getInvoiceDate())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        List<JournalLineRequest> lines = new ArrayList<>();
        for (ApInvoiceLine line : invoice.getLines()) {
            lines.add(new JournalLineRequest(line.getAccount().getId(), line.getAmount(), BigDecimal.ZERO,
                line.getDescription(), null));
        }
        // 대변: 공급업체 외상매입금 통제계정 = 총액
        lines.add(new JournalLineRequest(payables.getId(), BigDecimal.ZERO, invoice.getTotalAmount(),
            "외상매입금: " + invoice.getInvoiceNo(), null));

        JournalEntryCreateRequest request = new JournalEntryCreateRequest(
            invoice.getInvoiceDate(), period.getId(),
            "AP 전표 " + invoice.getInvoiceNo(), JournalEntryType.AP, invoice.getCurrency(), lines);

        Long journalEntryId = journalEntryService.createInternal(request).id();
        // GL 전표가 원천 문서(AP 전표)를 역참조하도록 연결(실무: 보조원장 ↔ GL 추적).
        journalEntryRepository.findById(journalEntryId)
            .ifPresent(je -> je.linkReference("AP_INVOICE", invoice.getId()));
        return journalEntryId;
    }

    /**
     * 지급 시 DRAFT 분개: <pre>(차) 외상매입금 [공급업체 통제계정]  (대) 현금·예금 [지급계정] = 지급액</pre>
     * 외상매입금 통제계정·지급계정이 없으면 전기하지 않고 {@code null}을 반환한다.
     */
    @Transactional
    public Long postPaymentDraft(ApInvoice invoice, BigDecimal amount, Account cashAccount, LocalDate paymentDate) {
        Account payables = invoice.getVendor().getPayablesAccount();
        if (payables == null || cashAccount == null) {
            return null;
        }
        FiscalPeriod period = fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(paymentDate, paymentDate)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        List<JournalLineRequest> lines = List.of(
            new JournalLineRequest(payables.getId(), amount, BigDecimal.ZERO,
                "외상매입금 지급: " + invoice.getInvoiceNo(), null),
            new JournalLineRequest(cashAccount.getId(), BigDecimal.ZERO, amount,
                "지급: " + invoice.getInvoiceNo(), null));

        JournalEntryCreateRequest request = new JournalEntryCreateRequest(
            paymentDate, period.getId(),
            "AP 지급 " + invoice.getInvoiceNo(), JournalEntryType.AP, invoice.getCurrency(), lines);

        Long journalEntryId = journalEntryService.createInternal(request).id();
        journalEntryRepository.findById(journalEntryId)
            .ifPresent(je -> je.linkReference("AP_PAYMENT", invoice.getId()));
        return journalEntryId;
    }
}
