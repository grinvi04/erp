package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeNo(String employeeNo);
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByStatus(EmployeeStatus status);
    boolean existsByEmployeeNo(String employeeNo);
    boolean existsByWorkEmail(String workEmail);
    boolean existsByPositionId(Long positionId);
    boolean existsByJobGradeId(Long jobGradeId);
}
