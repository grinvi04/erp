package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    Optional<LeavePolicy> findByCode(String code);
    boolean existsByCode(String code);
}
