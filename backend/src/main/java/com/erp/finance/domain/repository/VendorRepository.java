package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    boolean existsByCode(String code);

    // keyword는 서비스에서 소문자로 정규화되어 전달된다(LOWER(컬럼)과 매칭 → 대소문자 무시).
    @Query("SELECT v FROM Vendor v WHERE v.isActive = true AND "
            + "(:keyword IS NULL "
            + "OR LOWER(v.name) LIKE %:keyword% "
            + "OR LOWER(v.code) LIKE %:keyword% "
            + "OR LOWER(v.businessNo) LIKE %:keyword%)")
    Page<Vendor> search(@Param("keyword") String keyword, Pageable pageable);
}
