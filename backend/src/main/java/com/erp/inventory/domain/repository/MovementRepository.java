package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    boolean existsByMovementNo(String movementNo);
    long countByStatus(MovementStatus status);

    // 이동유형별 CONFIRMED 건수 — 빈 유형은 서비스에서 enum 0채움.
    @Query("SELECT m.movementType AS movementType, COUNT(m.id) AS count "
            + "FROM Movement m WHERE m.status = com.erp.inventory.domain.model.MovementStatus.CONFIRMED "
            + "GROUP BY m.movementType")
    List<MovementTypeCountRow> confirmedCountByType();

    @Query("SELECT m FROM Movement m WHERE "
            + "(:type IS NULL OR m.movementType = :type) AND "
            + "(:status IS NULL OR m.status = :status)")
    Page<Movement> findByTypeAndStatus(
            @Param("type") MovementType type,
            @Param("status") MovementStatus status,
            Pageable pageable);

    /**
     * 현재 사용자가 확정 결재할 수 있는 대기 조정 이동 — 통합 결재함 라우팅용.
     * 상태=대기, 유형=ADJUSTMENT, 작성자≠본인(직무분리). 재고는 금액 전결한도 미적용.
     * 테넌트 필터는 자동 적용.
     */
    @Query("SELECT m FROM Movement m WHERE m.status = :status "
            + "AND m.movementType = com.erp.inventory.domain.model.MovementType.ADJUSTMENT "
            + "AND m.createdBy <> :userId")
    List<Movement> findPendingApprovableBy(@Param("status") MovementStatus status,
                                           @Param("userId") String userId);
}
