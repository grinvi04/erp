package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ArInvoiceLine;
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
 * AR 전표 승인 → GL 자동 분개(DRAFT). 실무 표준 분개:
 * <pre>(차) 외상매출금 [고객 통제계정] 총액  (대) 매출·부가세예수금 [라인별 계정]</pre>
 * 차변 외상매출금은 전표가 아니라 고객 마스터의 통제계정에서 온다(AR 보조원장 ↔ GL 일치).
 * 전기(POST)는 회계담당이 별도 수행한다(여기선 DRAFT만 생성).
 *
 * <p>AR·GL은 같은 finance 모듈이라 직접 호출(모듈 간 이벤트 불필요).
 */
@Service
@RequiredArgsConstructor
public class ArInvoicePostingService {

    private final JournalEntryService journalEntryService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;

    /**
     * 승인된 AR 전표의 DRAFT 분개를 생성하고 분개 ID를 반환한다. 라인이 없거나 고객에
     * 외상매출금 통제계정이 설정되지 않았으면 전기하지 않고 {@code null}을 반환한다.
     *
     * <p>GL 분개 (AP와 반대):
     * <ul>
     *   <li>차변: 고객 외상매출금 통제계정 = 총액</li>
     *   <li>대변: 각 라인 계정(매출·부가세예수금 등) = 라인 금액</li>
     * </ul>
     */
    @Transactional
    public Long postDraft(ArInvoice invoice) {
        Account receivables = invoice.getCustomer().getReceivablesAccount();
        if (!invoice.hasLines() || receivables == null) {
            return null;
        }
        FiscalPeriod period = fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(invoice.getInvoiceDate(), invoice.getInvoiceDate())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        List<JournalLineRequest> lines = new ArrayList<>();
        // 차변: 고객 외상매출금 통제계정 = 총액
        lines.add(new JournalLineRequest(receivables.getId(), invoice.getTotalAmount(), BigDecimal.ZERO,
            "외상매출금: " + invoice.getInvoiceNo(), null));
        // 대변: 라인별 계정(매출·부가세예수금 등)
        for (ArInvoiceLine line : invoice.getLines()) {
            lines.add(new JournalLineRequest(line.getAccount().getId(), BigDecimal.ZERO, line.getAmount(),
                line.getDescription(), null));
        }

        JournalEntryCreateRequest request = new JournalEntryCreateRequest(
            invoice.getInvoiceDate(), period.getId(),
            "AR 전표 " + invoice.getInvoiceNo(), JournalEntryType.AR, invoice.getCurrency(), lines);

        Long journalEntryId = journalEntryService.createInternal(request).id();
        // GL 전표가 원천 문서(AR 전표)를 역참조하도록 연결(실무: 보조원장 ↔ GL 추적).
        journalEntryRepository.findById(journalEntryId)
            .ifPresent(je -> je.linkReference("AR_INVOICE", invoice.getId()));
        return journalEntryId;
    }

    /**
     * 수금 시 DRAFT 분개(AP 지급의 반전): <pre>(차) 현금·예금 [수금계정]  (대) 외상매출금 [고객 통제계정] = 수금액</pre>
     * 외상매출금 통제계정·수금계정이 없으면 전기하지 않고 {@code null}을 반환한다.
     */
    @Transactional
    public Long postPaymentDraft(ArInvoice invoice, BigDecimal amount, Account cashAccount, LocalDate paymentDate) {
        Account receivables = invoice.getCustomer().getReceivablesAccount();
        if (receivables == null || cashAccount == null) {
            return null;
        }
        FiscalPeriod period = fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(paymentDate, paymentDate)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        List<JournalLineRequest> lines = List.of(
            new JournalLineRequest(cashAccount.getId(), amount, BigDecimal.ZERO,
                "수금: " + invoice.getInvoiceNo(), null),
            new JournalLineRequest(receivables.getId(), BigDecimal.ZERO, amount,
                "외상매출금 회수: " + invoice.getInvoiceNo(), null));

        JournalEntryCreateRequest request = new JournalEntryCreateRequest(
            paymentDate, period.getId(),
            "AR 수금 " + invoice.getInvoiceNo(), JournalEntryType.AR, invoice.getCurrency(), lines);

        Long journalEntryId = journalEntryService.createInternal(request).id();
        journalEntryRepository.findById(journalEntryId)
            .ifPresent(je -> je.linkReference("AR_PAYMENT", invoice.getId()));
        return journalEntryId;
    }
}
