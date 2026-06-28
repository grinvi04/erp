package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.JobGrade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobGradeRepository extends JpaRepository<JobGrade, Long> {
  Optional<JobGrade> findByCode(String code);

  boolean existsByCode(String code);
}
