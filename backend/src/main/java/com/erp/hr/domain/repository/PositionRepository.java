package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Position;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {
  Optional<Position> findByCode(String code);

  boolean existsByCode(String code);

  // 직위별 ACTIVE 인원 — 인원 0 직위도 LEFT JOIN으로 보존(count=0), level_order 순.
  // 직위는 부서와 무관하게 전역이라 직원 스코프는 JOIN ON 조건으로 결합(WHERE 아님 — 빈 직위 보존).
  @Query(
      "SELECT p.id AS positionId, p.name AS positionName, COUNT(e.id) AS count "
          + "FROM Position p LEFT JOIN Employee e ON e.position = p "
          + "AND e.status = com.erp.hr.domain.model.EmployeeStatus.ACTIVE "
          + "AND (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds) "
          + "GROUP BY p.id, p.name, p.levelOrder ORDER BY p.levelOrder, p.id")
  List<PositionHeadcountRow> headcountByPosition(
      @Param("unscoped") boolean unscoped,
      @Param("selfUserId") String selfUserId,
      @Param("deptIds") Collection<Long> deptIds);
}
