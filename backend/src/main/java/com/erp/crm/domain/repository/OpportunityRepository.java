package com.erp.crm.domain.repository;

import com.erp.common.response.CurrencyAmount;
import com.erp.crm.domain.model.Opportunity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    @Query(value = "SELECT o FROM Opportunity o JOIN FETCH o.account a JOIN FETCH o.stage s WHERE "
            + "(:accountId IS NULL OR a.id = :accountId) AND "
            + "(:stageId IS NULL OR s.id = :stageId) AND "
            + "(:scoped = false OR o.ownerId IN :ownerIds)",
           countQuery = "SELECT COUNT(o) FROM Opportunity o WHERE "
            + "(:accountId IS NULL OR o.account.id = :accountId) AND "
            + "(:stageId IS NULL OR o.stage.id = :stageId) AND "
            + "(:scoped = false OR o.ownerId IN :ownerIds)")
    Page<Opportunity> search(@Param("accountId") Long accountId,
                             @Param("stageId") Long stageId,
                             @Param("scoped") boolean scoped,
                             @Param("ownerIds") java.util.Collection<String> ownerIds,
                             Pageable pageable);

    @Query("SELECT COUNT(o) FROM Opportunity o "
            + "WHERE o.stage.isClosedWon = false AND o.stage.isClosedLost = false AND "
            + "(:scoped = false OR o.ownerId IN :ownerIds)")
    long countOpen(@Param("scoped") boolean scoped,
                   @Param("ownerIds") java.util.Collection<String> ownerIds);

    @Query("SELECT new com.erp.common.response.CurrencyAmount("
            + "o.currency, COALESCE(SUM(o.amount), 0)) FROM Opportunity o "
            + "WHERE o.stage.isClosedWon = false AND o.stage.isClosedLost = false AND "
            + "(:scoped = false OR o.ownerId IN :ownerIds) "
            + "GROUP BY o.currency ORDER BY o.currency")
    List<CurrencyAmount> sumOpenAmountByCurrency(@Param("scoped") boolean scoped,
                                                 @Param("ownerIds") java.util.Collection<String> ownerIds);
}
