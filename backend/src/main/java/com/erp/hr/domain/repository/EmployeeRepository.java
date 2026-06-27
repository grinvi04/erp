package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository
    extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
  Optional<Employee> findByEmployeeNo(String employeeNo);

  List<Employee> findByDepartmentId(Long departmentId);

  List<Employee> findByStatus(EmployeeStatus status);

  long countByStatus(EmployeeStatus status);

  boolean existsByEmployeeNo(String employeeNo);

  boolean existsByWorkEmail(String workEmail);

  boolean existsByUserId(String userId);

  boolean existsByPositionId(Long positionId);

  boolean existsByJobGradeId(Long jobGradeId);

  @EntityGraph(attributePaths = {"department", "position", "jobGrade", "manager"})
  Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

  // 분석 집계 — 데이터 스코프는 파라미터로 결합(Specification 사용 불가). 빈 그룹은 서비스에서 enum 0채움.
  @Query(
      "SELECT e.status AS status, COUNT(e.id) AS count FROM Employee e "
          + "WHERE (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds) "
          + "GROUP BY e.status")
  List<EmployeeStatusCountRow> statusDistribution(
      @Param("unscoped") boolean unscoped,
      @Param("selfUserId") String selfUserId,
      @Param("deptIds") Collection<Long> deptIds);

  @Query(
      "SELECT e.employmentType AS employmentType, COUNT(e.id) AS count FROM Employee e "
          + "WHERE (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds) "
          + "GROUP BY e.employmentType")
  List<EmploymentTypeCountRow> employmentTypeDistribution(
      @Param("unscoped") boolean unscoped,
      @Param("selfUserId") String selfUserId,
      @Param("deptIds") Collection<Long> deptIds);

  @Query(
      "SELECT EXTRACT(MONTH FROM e.hireDate) AS month, COUNT(e.id) AS count FROM Employee e "
          + "WHERE EXTRACT(YEAR FROM e.hireDate) = :year "
          + "AND (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds) "
          + "GROUP BY EXTRACT(MONTH FROM e.hireDate)")
  List<MonthlyCountRow> monthlyHires(
      @Param("year") int year,
      @Param("unscoped") boolean unscoped,
      @Param("selfUserId") String selfUserId,
      @Param("deptIds") Collection<Long> deptIds);

  @Query(
      "SELECT EXTRACT(MONTH FROM e.terminationDate) AS month, COUNT(e.id) AS count FROM Employee e "
          + "WHERE e.terminationDate IS NOT NULL AND EXTRACT(YEAR FROM e.terminationDate) = :year "
          + "AND (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds) "
          + "GROUP BY EXTRACT(MONTH FROM e.terminationDate)")
  List<MonthlyCountRow> monthlyTerminations(
      @Param("year") int year,
      @Param("unscoped") boolean unscoped,
      @Param("selfUserId") String selfUserId,
      @Param("deptIds") Collection<Long> deptIds);
}
