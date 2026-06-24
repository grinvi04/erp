package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.Opportunity;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    @Query(value = "SELECT o FROM Opportunity o JOIN FETCH o.account a JOIN FETCH o.stage s WHERE "
            + "(:accountId IS NULL OR a.id = :accountId) AND "
            + "(:stageId IS NULL OR s.id = :stageId)",
           countQuery = "SELECT COUNT(o) FROM Opportunity o WHERE "
            + "(:accountId IS NULL OR o.account.id = :accountId) AND "
            + "(:stageId IS NULL OR o.stage.id = :stageId)")
    Page<Opportunity> search(@Param("accountId") Long accountId,
                             @Param("stageId") Long stageId,
                             Pageable pageable);

    @Query("SELECT COUNT(o) FROM Opportunity o "
            + "WHERE o.stage.isClosedWon = false AND o.stage.isClosedLost = false")
    long countOpen();

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o "
            + "WHERE o.stage.isClosedWon = false AND o.stage.isClosedLost = false")
    BigDecimal sumOpenAmount();
}
