package com.erp.finance.application.service;

import com.erp.common.currency.CurrencyConversionPort;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.BaseCurrencyService.FxGainLossAccounts;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FxGainLoss;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외화 결제(AP 지급·AR 수금) 분개 라인 구성 — 실현 환차손익 라인 포함. AP/AR 공용(부호만 반전).
 *
 * <p><b>환차 적용</b>(외화 AND 인보이스환율 있음 AND 결제일 환율 있음 AND 환차손익 계정 설정됨): 분개를
 * 기준통화로 기록한다 — 통제계정@인보이스환율(원장 정확 청산)·현금@결제환율·차액@환차손익 계정. AP는
 * 결제환율&gt;인보이스환율이면 (차)환차손, 낮으면 (대)환차이익. AR은 부호 반전.
 *
 * <p><b>폴백</b>(위 조건 하나라도 불충족): 기존 원통화 2라인을 그대로 반환(환차 없음, 결제 차단 안 함)
 * + 사유 로그(event=FX_GAINLOSS_SKIPPED). 차대변은 두 반올림 금액의 차로 환차를 잡아 항상 균형.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxPaymentJournalFactory {

    /** 결제 종류 — 통제계정·현금의 차대변 방향과 환차 부호 해석이 반전된다. */
    public enum Side { AP, AR }

    /** 구성된 결제 분개 라인과 그 분개 통화(환차 적용 시 기준통화, 폴백 시 원통화). */
    public record PaymentLines(List<JournalLineRequest> lines, String currency) {}

    private final CurrencyConverter currencyConverter;
    private final BaseCurrencyService baseCurrencyService;

    /**
     * 결제 분개 라인을 구성한다. {@code invoiceRate}는 인보이스 스냅샷 환율, {@code amount}는 결제(원통화) 금액.
     * {@code control}은 통제계정(AP=외상매입금/AR=외상매출금), {@code cash}는 현금·예금 계정.
     */
    public PaymentLines build(Side side, String invoiceNo, String invoiceCurrency, BigDecimal invoiceRate,
                              Account control, Account cash, BigDecimal amount, LocalDate paymentDate) {
        String baseCurrency = currencyConverter.baseCurrencyCode();

        if (invoiceCurrency.equals(baseCurrency)) {
            return fallback(side, invoiceNo, control, cash, amount, invoiceCurrency, "BASE_CURRENCY");
        }
        if (invoiceRate == null) {
            return fallback(side, invoiceNo, control, cash, amount, invoiceCurrency, "NO_INVOICE_RATE");
        }
        Optional<FxGainLossAccounts> fxAccounts = baseCurrencyService.currentFxAccounts();
        if (fxAccounts.isEmpty()) {
            return fallback(side, invoiceNo, control, cash, amount, invoiceCurrency, "FX_ACCOUNTS_NOT_CONFIGURED");
        }
        Optional<CurrencyConversionPort.Conversion> paymentConversion =
            currencyConverter.tryConvert(amount, invoiceCurrency, paymentDate);
        if (paymentConversion.isEmpty()) {
            return fallback(side, invoiceNo, control, cash, amount, invoiceCurrency, "NO_PAYMENT_RATE");
        }

        FxGainLoss fx = FxGainLoss.of(amount, invoiceRate, paymentConversion.get().rate());
        List<JournalLineRequest> lines = new ArrayList<>();
        if (side == Side.AP) {
            // (차)외상매입금 = 인보이스환율 청산  /  (대)현금 = 결제환율
            lines.add(debit(control, fx.controlAmount(), "외상매입금 지급: " + invoiceNo));
            lines.add(credit(cash, fx.cashAmount(), "지급: " + invoiceNo));
            if (fx.hasDifference()) {
                lines.add(fx.cashExceedsControl()
                    // 더 지급 → 환차손(비용·차변)
                    ? debit(fxAccounts.get().lossAccount(), fx.amount(), "외환차손: " + invoiceNo)
                    // 덜 지급 → 환차이익(수익·대변)
                    : credit(fxAccounts.get().gainAccount(), fx.amount(), "외환차이익: " + invoiceNo));
            }
        } else {
            // (차)현금 = 결제환율  /  (대)외상매출금 = 인보이스환율 청산
            lines.add(debit(cash, fx.cashAmount(), "수금: " + invoiceNo));
            lines.add(credit(control, fx.controlAmount(), "외상매출금 회수: " + invoiceNo));
            if (fx.hasDifference()) {
                lines.add(fx.cashExceedsControl()
                    // 더 수금 → 환차이익(수익·대변)
                    ? credit(fxAccounts.get().gainAccount(), fx.amount(), "외환차이익: " + invoiceNo)
                    // 덜 수금 → 환차손(비용·차변)
                    : debit(fxAccounts.get().lossAccount(), fx.amount(), "외환차손: " + invoiceNo));
            }
        }
        return new PaymentLines(lines, baseCurrency);
    }

    /** 폴백 — 기존 원통화 2라인(환차 없음). 사유를 로그로 남긴다(원장 미청산 가능성 가시화). */
    private PaymentLines fallback(Side side, String invoiceNo, Account control, Account cash,
                                  BigDecimal amount, String invoiceCurrency, String reason) {
        log.atInfo().addKeyValue("event", "FX_GAINLOSS_SKIPPED")
            .addKeyValue("side", side)
            .addKeyValue("invoiceNo", invoiceNo)
            .addKeyValue("currency", invoiceCurrency)
            .addKeyValue("reason", reason)
            .log("환차손익 분개 생략 — 기존 원통화 결제 분개 유지");
        List<JournalLineRequest> lines = side == Side.AP
            ? List.of(
                debit(control, amount, "외상매입금 지급: " + invoiceNo),
                credit(cash, amount, "지급: " + invoiceNo))
            : List.of(
                debit(cash, amount, "수금: " + invoiceNo),
                credit(control, amount, "외상매출금 회수: " + invoiceNo));
        return new PaymentLines(lines, invoiceCurrency);
    }

    private static JournalLineRequest debit(Account account, BigDecimal amount, String description) {
        return new JournalLineRequest(account.getId(), amount, BigDecimal.ZERO, description, null);
    }

    private static JournalLineRequest credit(Account account, BigDecimal amount, String description) {
        return new JournalLineRequest(account.getId(), BigDecimal.ZERO, amount, description, null);
    }
}
