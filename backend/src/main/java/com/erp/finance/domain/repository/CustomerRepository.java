package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  boolean existsByCode(String code);

  // keyword는 서비스에서 소문자로 정규화되어 전달된다(LOWER(컬럼)과 매칭 → 대소문자 무시).
  @Query(
      "SELECT c FROM Customer c WHERE c.isActive = true AND "
          + "(:keyword IS NULL "
          + "OR LOWER(c.name) LIKE %:keyword% "
          + "OR LOWER(c.code) LIKE %:keyword% "
          + "OR LOWER(c.businessNo) LIKE %:keyword%)")
  Page<Customer> search(@Param("keyword") String keyword, Pageable pageable);
}
