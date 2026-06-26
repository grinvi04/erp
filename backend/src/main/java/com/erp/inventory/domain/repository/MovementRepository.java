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
}
