package com.erp.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.dto.EmployeeTerminateRequest;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원 생명주기 변경(채용·발령·퇴직 등)이 감사 로그에 누가·무엇을·어떤 이벤트로 기록되는지 실 컨텍스트로 검증한다 — 인사 데이터 변경은 노동/컴플라이언스 감사 대상.
 */
@Transactional
class EmployeeAuditIntegrationTest extends AbstractIntegrationTest {

  @Autowired private EmployeeService employeeService;
  @Autowired private DepartmentRepository departmentRepository;
  @Autowired private PositionRepository positionRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  private Long deptId;
  private Long posId;

  @BeforeEach
  void setUp() {
    deptId = departmentRepository.save(Department.createRoot("DEV", "개발팀")).getId();
    posId = positionRepository.save(Position.of("P001", "Engineer", 2)).getId();
    authenticate("hr-admin", "hr:employee:write");
  }

  private EmployeeResponse createEmployee() {
    return employeeService.create(
        new EmployeeCreateRequest(
            "EMP-100",
            "김",
            "직원",
            LocalDate.of(1990, 1, 1),
            PersonalInfo.Gender.MALE,
            null,
            null,
            null,
            deptId,
            posId,
            null,
            LocalDate.of(2024, 1, 1),
            EmploymentType.REGULAR,
            "emp100@test.com",
            BigDecimal.valueOf(50000000),
            null));
  }

  @Test
  void create_writesHireAuditLog() {
    EmployeeResponse created = createEmployee();

    List<AuditLog> logs =
        auditLogRepository
            .search(TEST_TENANT_ID, "EMPLOYEE", created.id(), "hr-admin", PageRequest.of(0, 10))
            .getContent();

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo(AuditLog.AuditAction.CREATE);
    assertThat(logs.get(0).getAfterData()).contains("HIRE");
    assertThat(logs.get(0).getPerformedBy()).isEqualTo("hr-admin");
  }

  @Test
  void terminate_writesTerminateAuditLog() {
    EmployeeResponse created = createEmployee();

    employeeService.terminate(
        created.id(), new EmployeeTerminateRequest(LocalDate.of(2026, 1, 31)));

    List<AuditLog> logs =
        auditLogRepository
            .search(TEST_TENANT_ID, "EMPLOYEE", created.id(), "hr-admin", PageRequest.of(0, 10))
            .getContent();

    // 같은 트랜잭션 내 동일 타임스탬프라 순서 비의존 — 두 이벤트 모두 기록됐는지 확인.
    assertThat(logs).hasSize(2);
    assertThat(logs)
        .anyMatch(
            l ->
                l.getAction() == AuditLog.AuditAction.UPDATE
                    && l.getAfterData().contains("TERMINATE"));
    assertThat(logs)
        .anyMatch(
            l -> l.getAction() == AuditLog.AuditAction.CREATE && l.getAfterData().contains("HIRE"));
  }
}
