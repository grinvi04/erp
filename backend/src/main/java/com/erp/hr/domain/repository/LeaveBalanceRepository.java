package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.LeaveBalance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
  Optional<LeaveBalance> findByEmployeeIdAndLeavePolicyIdAndYear(
      Long employeeId, Long leavePolicyId, int year);

  List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
}
