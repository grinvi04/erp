package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeIdAndLeavePolicyIdAndYear(Long employeeId, Long leavePolicyId, int year);
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
}
