package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.PositionCreateRequest;
import com.erp.hr.application.dto.PositionResponse;
import com.erp.hr.application.dto.PositionUpdateRequest;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionService {

  private final PositionRepository positionRepository;
  private final EmployeeRepository employeeRepository;
  private final PermissionChecker permissionChecker;

  public List<PositionResponse> findAll() {
    permissionChecker.require(Permission.HR_POSITION_READ);
    return positionRepository.findAll().stream()
        .sorted(Comparator.comparingInt(Position::getLevelOrder))
        .map(PositionResponse::from)
        .toList();
  }

  public PositionResponse findById(Long id) {
    permissionChecker.require(Permission.HR_POSITION_READ);
    return PositionResponse.from(getOrThrow(id));
  }

  @Transactional
  public PositionResponse create(PositionCreateRequest request) {
    permissionChecker.require(Permission.HR_POSITION_WRITE);
    if (positionRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    Position position = Position.of(request.code(), request.name(), request.levelOrder());
    return PositionResponse.from(positionRepository.save(position));
  }

  @Transactional
  public PositionResponse update(Long id, PositionUpdateRequest request) {
    permissionChecker.require(Permission.HR_POSITION_WRITE);
    Position position = getOrThrow(id);
    position.checkVersion(request.version());
    position.update(request.name(), request.levelOrder());
    positionRepository.flush();
    return PositionResponse.from(position);
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.HR_POSITION_WRITE);
    Position position = getOrThrow(id);
    if (employeeRepository.existsByPositionId(id)) {
      throw new ErpException(ErrorCode.POSITION_IN_USE);
    }
    position.softDelete();
  }

  private Position getOrThrow(Long id) {
    return positionRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.POSITION_NOT_FOUND));
  }
}
