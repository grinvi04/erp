package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @Query(value = "SELECT l FROM Lead l LEFT JOIN FETCH l.convertedAccount WHERE "
            + "(:status IS NULL OR l.status = :status) AND "
            + "(:keyword IS NULL OR l.lastName LIKE %:keyword% OR l.firstName LIKE %:keyword% "
            + "OR l.company LIKE %:keyword%)",
           countQuery = "SELECT COUNT(l) FROM Lead l WHERE "
            + "(:status IS NULL OR l.status = :status) AND "
            + "(:keyword IS NULL OR l.lastName LIKE %:keyword% OR l.firstName LIKE %:keyword% "
            + "OR l.company LIKE %:keyword%)")
    Page<Lead> search(@Param("status") LeadStatus status,
                      @Param("keyword") String keyword,
                      Pageable pageable);
}
