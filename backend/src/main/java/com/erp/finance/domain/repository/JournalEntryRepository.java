package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    boolean existsByEntryNo(String entryNo);

    // 기준통화 변경 가드 — base_amount 스냅샷이 하나라도 있으면 변경 거부(현재 테넌트는 @TenantId 자동 필터).
    boolean existsByBaseAmountIsNotNull();

    // JournalEntryResponse.from은 fiscalPeriod를 역참조 — 목록 매핑 시 N+1 방지를 위해
    // fiscalPeriod(@ManyToOne, LAZY)를 페이지 쿼리에서 함께 페치한다. 카운트는 페치 없는 별도 countQuery 유지.
    @Query(value = "SELECT j FROM JournalEntry j LEFT JOIN FETCH j.fiscalPeriod p WHERE p.id = :fiscalPeriodId",
        countQuery = "SELECT COUNT(j) FROM JournalEntry j WHERE j.fiscalPeriod.id = :fiscalPeriodId")
    Page<JournalEntry> findByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId, Pageable pageable);

    @Query(value = "SELECT j FROM JournalEntry j LEFT JOIN FETCH j.fiscalPeriod WHERE j.status = :status",
        countQuery = "SELECT COUNT(j) FROM JournalEntry j WHERE j.status = :status")
    Page<JournalEntry> findByStatus(@Param("status") JournalEntryStatus status, Pageable pageable);

    long countByStatus(JournalEntryStatus status);

    @Query(value = "SELECT j FROM JournalEntry j LEFT JOIN FETCH j.fiscalPeriod p "
            + "WHERE p.id = :fiscalPeriodId AND j.status = :status",
        countQuery = "SELECT COUNT(j) FROM JournalEntry j "
            + "WHERE j.fiscalPeriod.id = :fiscalPeriodId AND j.status = :status")
    Page<JournalEntry> findByFiscalPeriodIdAndStatus(@Param("fiscalPeriodId") Long fiscalPeriodId,
                                                     @Param("status") JournalEntryStatus status, Pageable pageable);
    Optional<JournalEntry> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * 전결규정상 현재 사용자가 전기 결재할 수 있는 대기 전표 — 통합 결재함 라우팅용.
     * 상태=대기, 작성자≠본인(직무분리), 차변 합계≤본인 전결 한도. 테넌트 필터는 자동 적용.
     */
    @Query("SELECT j FROM JournalEntry j LEFT JOIN FETCH j.fiscalPeriod "
            + "WHERE j.status = :status AND j.createdBy <> :userId AND j.totalDebit <= :limit")
    java.util.List<JournalEntry> findPendingApprovableBy(@Param("status") JournalEntryStatus status,
                                                         @Param("userId") String userId,
                                                         @Param("limit") java.math.BigDecimal limit);
}
