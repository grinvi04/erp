package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByCode(String code);
    Page<Customer> findByIsActiveTrue(Pageable pageable);
}
