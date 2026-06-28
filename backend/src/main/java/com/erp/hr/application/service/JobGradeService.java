package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.JobGradeCreateRequest;
import com.erp.hr.application.dto.JobGradeResponse;
import com.erp.hr.application.dto.JobGradeUpdateRequest;
import com.erp.hr.domain.model.JobGrade;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.JobGradeRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobGradeService {

  private final JobGradeRepository jobGradeRepository;
  private final EmployeeRepository employeeRepository;
  private final PermissionChecker permissionChecker;

  public List<JobGradeResponse> findAll() {
    permissionChecker.require(Permission.HR_JOBGRADE_READ);
    return jobGradeRepository.findAll().stream()
        .sorted(Comparator.comparingInt(JobGrade::getGradeOrder))
        .map(JobGradeResponse::from)
        .toList();
  }

  public JobGradeResponse findById(Long id) {
    permissionChecker.require(Permission.HR_JOBGRADE_READ);
    return JobGradeResponse.from(getOrThrow(id));
  }

  @Transactional
  public JobGradeResponse create(JobGradeCreateRequest request) {
    permissionChecker.require(Permission.HR_JOBGRADE_WRITE);
    if (jobGradeRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    JobGrade grade =
        JobGrade.of(
            request.code(),
            request.name(),
            request.gradeOrder(),
            request.minSalary(),
            request.maxSalary());
    return JobGradeResponse.from(jobGradeRepository.save(grade));
  }

  @Transactional
  public JobGradeResponse update(Long id, JobGradeUpdateRequest request) {
    permissionChecker.require(Permission.HR_JOBGRADE_WRITE);
    JobGrade grade = getOrThrow(id);
    grade.checkVersion(request.version());
    grade.update(request.name(), request.gradeOrder(), request.minSalary(), request.maxSalary());
    jobGradeRepository.flush();
    return JobGradeResponse.from(grade);
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.HR_JOBGRADE_WRITE);
    JobGrade grade = getOrThrow(id);
    if (employeeRepository.existsByJobGradeId(id)) {
      throw new ErpException(ErrorCode.JOB_GRADE_IN_USE);
    }
    grade.softDelete();
  }

  private JobGrade getOrThrow(Long id) {
    return jobGradeRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.JOB_GRADE_NOT_FOUND));
  }
}
