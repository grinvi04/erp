package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.Activity;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    // LEFT JOIN FETCH: account/contact/opportunity are nullable — INNER JOIN would silently
    // exclude activities with null FKs even when the filter param is null (no filter).
    // DISTINCT prevents duplicate rows from multi-path join.
    @Query(value = "SELECT DISTINCT a FROM Activity a "
            + "LEFT JOIN FETCH a.account acc "
            + "LEFT JOIN FETCH a.contact con "
            + "LEFT JOIN FETCH a.opportunity opp "
            + "WHERE (:opportunityId IS NULL OR opp.id = :opportunityId) AND "
            + "(:accountId IS NULL OR acc.id = :accountId) AND "
            + "(:activityType IS NULL OR a.activityType = :activityType) AND "
            + "(:status IS NULL OR a.status = :status)",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Activity a "
            + "LEFT JOIN a.account acc "
            + "LEFT JOIN a.contact con "
            + "LEFT JOIN a.opportunity opp "
            + "WHERE (:opportunityId IS NULL OR opp.id = :opportunityId) AND "
            + "(:accountId IS NULL OR acc.id = :accountId) AND "
            + "(:activityType IS NULL OR a.activityType = :activityType) AND "
            + "(:status IS NULL OR a.status = :status)")
    Page<Activity> search(@Param("opportunityId") Long opportunityId,
                          @Param("accountId") Long accountId,
                          @Param("activityType") ActivityType activityType,
                          @Param("status") ActivityStatus status,
                          Pageable pageable);
}
