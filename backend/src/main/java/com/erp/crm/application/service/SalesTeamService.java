package com.erp.crm.application.service;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.domain.model.SalesTeam;
import com.erp.crm.domain.repository.SalesTeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영업팀(SalesTeam) 관리 — 팀과 팀원(사용자 sub)을 관리한다. CRM DataScope의 DEPARTMENT 스코프 기준이 되는 조직 데이터이므로, 읽기는
 * iam:read·쓰기는 iam:write 권한을 요구하고(IamService 패턴), 변경은 감사 로그에 남긴다. 테넌트 격리는 BaseEntity의 @TenantId
 * 자동필터에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesTeamService {

  private final SalesTeamRepository salesTeamRepository;
  private final PermissionChecker permissionChecker;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public List<SalesTeamResponse> listTeams() {
    permissionChecker.require(Permission.IAM_READ);
    return salesTeamRepository.findAllByOrderByCodeAsc().stream()
        .map(SalesTeamResponse::from)
        .toList();
  }

  public SalesTeamResponse getTeam(Long id) {
    permissionChecker.require(Permission.IAM_READ);
    return SalesTeamResponse.from(getOrThrow(id));
  }

  @Transactional
  public SalesTeamResponse createTeam(String code, String name) {
    permissionChecker.require(Permission.IAM_WRITE);
    if (salesTeamRepository.existsByCode(code)) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    SalesTeam saved = salesTeamRepository.save(SalesTeam.of(code, name));
    auditService.record(
        "SALES_TEAM",
        saved.getId(),
        AuditLog.AuditAction.CREATE,
        null,
        json(Map.of("code", saved.getCode(), "name", saved.getName())));
    return SalesTeamResponse.from(saved);
  }

  @Transactional
  public SalesTeamResponse updateTeam(Long id, String name, Long version) {
    permissionChecker.require(Permission.IAM_WRITE);
    SalesTeam team = getOrThrow(id);
    team.checkVersion(version);
    team.rename(name);
    auditService.record(
        "SALES_TEAM",
        team.getId(),
        AuditLog.AuditAction.UPDATE,
        null,
        json(Map.of("name", team.getName())));
    return SalesTeamResponse.from(team);
  }

  @Transactional
  public void deleteTeam(Long id) {
    permissionChecker.require(Permission.IAM_WRITE);
    SalesTeam team = getOrThrow(id);
    salesTeamRepository.delete(team);
    auditService.record(
        "SALES_TEAM", id, AuditLog.AuditAction.DELETE, null, json(Map.of("code", team.getCode())));
  }

  @Transactional
  public SalesTeamResponse addMember(Long teamId, String userId) {
    permissionChecker.require(Permission.IAM_WRITE);
    SalesTeam team = getOrThrow(teamId);
    team.addMember(userId);
    auditService.record(
        "SALES_TEAM", teamId, AuditLog.AuditAction.UPDATE, null, json(Map.of("addMember", userId)));
    return SalesTeamResponse.from(team);
  }

  @Transactional
  public SalesTeamResponse removeMember(Long teamId, String userId) {
    permissionChecker.require(Permission.IAM_WRITE);
    SalesTeam team = getOrThrow(teamId);
    team.removeMember(userId);
    auditService.record(
        "SALES_TEAM",
        teamId,
        AuditLog.AuditAction.UPDATE,
        null,
        json(Map.of("removeMember", userId)));
    return SalesTeamResponse.from(team);
  }

  private SalesTeam getOrThrow(Long id) {
    return salesTeamRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /** 감사 afterData JSON 안전 직렬화 — userId 등 외부 입력의 따옴표가 jsonb를 깨지 않도록 Jackson 사용. */
  private String json(Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      return null; // afterData는 부가정보 — 직렬화 실패 시 생략(감사 자체는 기록)
    }
  }
}
