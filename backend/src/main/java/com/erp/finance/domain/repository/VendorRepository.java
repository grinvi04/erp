package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    boolean existsByCode(String code);
    Page<Vendor> findByIsActiveTrue(Pageable pageable);
}
