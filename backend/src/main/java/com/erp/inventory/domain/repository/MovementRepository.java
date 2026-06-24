package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    boolean existsByMovementNo(String movementNo);
    long countByStatus(MovementStatus status);

    @Query("SELECT m FROM Movement m WHERE "
            + "(:type IS NULL OR m.movementType = :type) AND "
            + "(:status IS NULL OR m.status = :status)")
    Page<Movement> findByTypeAndStatus(
            @Param("type") MovementType type,
            @Param("status") MovementStatus status,
            Pageable pageable);
}
