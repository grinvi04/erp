package com.erp.common.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 원통화 금액을 테넌트 기준통화로 환산하는 포트(SPI) — 모듈 경계용.
 *
 * <p>환산 구현은 finance 모듈에 있다(환율·기준통화 마스터 소유). CRM 등 타 모듈은 finance를
 * 직접 참조하지 않고 이 common 포트만 주입해 사용한다(PendingApprovalContributor와 동일 패턴).
 *
 * <p>거래 시점 스냅샷(PR2)에 쓰인다. 환율이 없으면 예외 대신 {@link Optional#empty()}를 반환해
 * 거래는 정상 생성하되 base_amount를 미산정(null)으로 남긴다(조용한 0·1 금지).
 */
public interface CurrencyConversionPort {

    /**
     * amount(currency)를 date 시점 기준통화로 환산한다.
     * 기준통화면 (amount, rate=1), 외화면 환율 적용, 환율 부재(또는 amount=null)면 빈 Optional.
     */
    Optional<Conversion> tryConvert(BigDecimal amount, String currency, LocalDate date);

    /** 환산 결과 — 기준통화 환산액과 적용 환율. */
    record Conversion(BigDecimal baseAmount, BigDecimal rate) {}
}
