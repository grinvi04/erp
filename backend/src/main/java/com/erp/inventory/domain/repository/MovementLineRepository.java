package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.MovementLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementLineRepository extends JpaRepository<MovementLine, Long> {
    List<MovementLine> findByMovement_IdOrderByLineNoAsc(Long movementId);

    // MovementLineResponse.from은 item·fromLocation·toLocation(모두 @ManyToOne, LAZY)을 역참조.
    // 배치(라인 IN 조회)라 라인 자체는 이미 단일 쿼리 — 각 라인의 to-one만 페치하므로 카테시안 곱 없음.
    // item은 optional=false라 JOIN FETCH(내부), 위치는 nullable이라 LEFT JOIN FETCH로 라인 누락 방지.
    @Query("SELECT l FROM MovementLine l "
            + "JOIN FETCH l.item "
            + "LEFT JOIN FETCH l.fromLocation "
            + "LEFT JOIN FETCH l.toLocation "
            + "WHERE l.movement.id IN :movementIds ORDER BY l.lineNo ASC")
    List<MovementLine> findByMovement_IdInOrderByLineNoAsc(@Param("movementIds") List<Long> movementIds);
}
