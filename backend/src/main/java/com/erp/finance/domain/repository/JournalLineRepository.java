package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByJournalEntryIdOrderByLineNoAsc(Long journalEntryId);

    /**
     * 계정별 차·대변 환산합(기간) — 시산표·손익계산서용. POSTED·환율 산정(exchangeRate not null) 분개만,
     * 라인 금액을 그 분개의 exchangeRate로 곱해 기준통화로 집계한다(KRW·rate=1이면 원액).
     * 합계는 원시(full precision)로 반환한다 — 한 분개 내 모든 라인이 동일 exchangeRate이므로
     * Σ(debit×rate)==Σ(credit×rate)가 정확히 성립한다. 표시용 반올림은 서비스가 합계·균형 산정 후에만 적용한다
     * (계정별 ROUND 후 재합산하면 Σ ROUND(x) ≠ ROUND(Σ x)로 총차변≠총대변 오차가 난다).
     */
    @Query("SELECT l.account.id AS accountId, "
            + "COALESCE(SUM(l.debitAmount * e.exchangeRate), 0) AS debitSum, "
            + "COALESCE(SUM(l.creditAmount * e.exchangeRate), 0) AS creditSum "
            + "FROM JournalLine l JOIN l.journalEntry e "
            + "WHERE e.status = com.erp.finance.domain.model.JournalEntryStatus.POSTED "
            + "AND e.exchangeRate IS NOT NULL "
            + "AND e.entryDate >= :start AND e.entryDate <= :end "
            + "GROUP BY l.account.id")
    List<AccountBalanceRow> aggregateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 계정별 차·대변 환산합(누적) — 재무상태표용. 기초~기준일(asOf)까지 POSTED·환율 산정 분개를 누적 집계한다.
     * 집계 규칙은 {@link #aggregateBetween}과 동일하되(원시 합계 반환) 하한 없이 기준일 이전 전체를 합산한다.
     */
    @Query("SELECT l.account.id AS accountId, "
            + "COALESCE(SUM(l.debitAmount * e.exchangeRate), 0) AS debitSum, "
            + "COALESCE(SUM(l.creditAmount * e.exchangeRate), 0) AS creditSum "
            + "FROM JournalLine l JOIN l.journalEntry e "
            + "WHERE e.status = com.erp.finance.domain.model.JournalEntryStatus.POSTED "
            + "AND e.exchangeRate IS NOT NULL "
            + "AND e.entryDate <= :asOf "
            + "GROUP BY l.account.id")
    List<AccountBalanceRow> aggregateUpTo(@Param("asOf") LocalDate asOf);
}
