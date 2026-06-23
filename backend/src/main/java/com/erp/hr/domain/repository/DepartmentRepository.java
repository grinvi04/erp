package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    List<Department> findByParentId(Long parentId);
    List<Department> findByParentIsNull();
    boolean existsByCode(String code);
}
