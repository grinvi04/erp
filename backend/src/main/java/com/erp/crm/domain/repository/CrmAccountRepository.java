package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmAccountRepository extends JpaRepository<Account, Long> {
    boolean existsByCode(String code);

    @Query("SELECT a FROM CrmAccount a WHERE "
            + "(:keyword IS NULL OR a.name LIKE %:keyword% OR a.code LIKE %:keyword%) AND "
            + "(:isActive IS NULL OR a.isActive = :isActive)")
    Page<Account> search(@Param("keyword") String keyword,
                         @Param("isActive") Boolean isActive,
                         Pageable pageable);
}
