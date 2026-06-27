package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Department;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  Optional<Department> findByCode(String code);

  List<Department> findByParentId(Long parentId);

  List<Department> findByParentIsNull();

  boolean existsByCode(String code);

  // 부서별 ACTIVE 인원 — 인원 0 부서도 LEFT JOIN으로 보존(count=0). 부서 차원에 스코프를 걸어
  // 스코프 밖 부서는 노출하지 않는다(스코프 내 빈 부서는 보존). e.department=d 이므로 직원 스코프는 부서 스코프로 충족.
  @Query(
      "SELECT d.id AS departmentId, d.name AS departmentName, COUNT(e.id) AS count "
          + "FROM Department d LEFT JOIN Employee e ON e.department = d "
          + "AND e.status = com.erp.hr.domain.model.EmployeeStatus.ACTIVE "
          + "WHERE (:unscoped = true OR d.id IN :deptIds) "
          + "GROUP BY d.id, d.name, d.sortOrder ORDER BY d.sortOrder, d.id")
  List<DepartmentHeadcountRow> headcountByDepartment(
      @Param("unscoped") boolean unscoped, @Param("deptIds") Collection<Long> deptIds);
}
