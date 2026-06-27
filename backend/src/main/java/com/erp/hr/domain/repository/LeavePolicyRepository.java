package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.LeavePolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
  Optional<LeavePolicy> findByCode(String code);

  boolean existsByCode(String code);
}
