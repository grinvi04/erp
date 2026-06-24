package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.MovementLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementLineRepository extends JpaRepository<MovementLine, Long> {
    List<MovementLine> findByMovement_IdOrderByLineNoAsc(Long movementId);
    List<MovementLine> findByMovement_IdInOrderByLineNoAsc(List<Long> movementIds);
}
