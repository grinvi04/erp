package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.dto.DepartmentUpdateRequest;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

  private final DepartmentRepository departmentRepository;
  private final EmployeeRepository employeeRepository;
  private final PermissionChecker permissionChecker;

  public List<DepartmentResponse> findAll() {
    permissionChecker.require(Permission.HR_DEPARTMENT_READ);
    return departmentRepository.findAll().stream().map(DepartmentResponse::from).toList();
  }

  public DepartmentResponse findById(Long id) {
    permissionChecker.require(Permission.HR_DEPARTMENT_READ);
    return DepartmentResponse.from(getOrThrow(id));
  }

  @Transactional
  public DepartmentResponse create(DepartmentCreateRequest request) {
    permissionChecker.require(Permission.HR_DEPARTMENT_WRITE);
    if (departmentRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    Department dept;
    if (request.parentId() != null) {
      Department parent = getOrThrow(request.parentId());
      dept = Department.createChild(request.code(), request.name(), parent, request.sortOrder());
    } else {
      dept = Department.createRoot(request.code(), request.name());
    }
    return DepartmentResponse.from(departmentRepository.save(dept));
  }

  @Transactional
  public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
    permissionChecker.require(Permission.HR_DEPARTMENT_WRITE);
    Department dept = getOrThrow(id);
    dept.checkVersion(request.version());
    dept.rename(request.name());

    Long currentParentId = dept.getParent() != null ? dept.getParent().getId() : null;
    if (!java.util.Objects.equals(currentParentId, request.parentId())) {
      Department newParent = request.parentId() != null ? getOrThrow(request.parentId()) : null;
      verifyNoCycle(dept, newParent);
      dept.changeParent(newParent);
      cascadeDepth(dept.getId());
    }

    if (request.active()) {
      dept.activate();
    } else {
      dept.deactivate();
    }

    departmentRepository.flush();
    return DepartmentResponse.from(dept);
  }

  /** 순환 방지 — 새 상위가 자기 자신이거나 자기 하위(조상 체인에 자신이 등장)이면 거부한다. */
  private void verifyNoCycle(Department dept, Department newParent) {
    for (Department ancestor = newParent; ancestor != null; ancestor = ancestor.getParent()) {
      if (ancestor.getId().equals(dept.getId())) {
        throw new ErpException(ErrorCode.DEPARTMENT_CYCLE);
      }
    }
  }

  /** 상위 이동 후 하위 트리의 depth를 재귀적으로 보정한다. */
  private void cascadeDepth(Long parentId) {
    for (Department child : departmentRepository.findByParentId(parentId)) {
      child.refreshDepthFromParent();
      cascadeDepth(child.getId());
    }
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.HR_DEPARTMENT_WRITE);
    Department dept = getOrThrow(id);
    if (!departmentRepository.findByParentId(id).isEmpty()) {
      throw new ErpException(ErrorCode.DEPARTMENT_HAS_CHILDREN);
    }
    if (!employeeRepository.findByDepartmentId(id).isEmpty()) {
      throw new ErpException(ErrorCode.DEPARTMENT_HAS_MEMBERS);
    }
    dept.softDelete();
  }

  private Department getOrThrow(Long id) {
    return departmentRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.DEPARTMENT_NOT_FOUND));
  }
}
