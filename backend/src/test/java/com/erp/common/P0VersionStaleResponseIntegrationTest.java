package com.erp.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.dto.DepartmentUpdateRequest;
import com.erp.hr.application.service.DepartmentService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 버그1 재현(RED) — UPDATE 응답이 stale version을 돌려준다.
 *
 * <p>서비스 {@code update()}가 flush 전에 {@code XxxResponse.from(entity)}로 매핑하므로, 갱신 응답 DTO의 version이 증가
 * 전 값(예: 0)으로 나간다. 실제 DB·후속 조회 version은 1로 증가해 있어 클라이언트가 다음 read-modify-write에서 stale version을 다시
 * 보내 거짓 낙관적 잠금 충돌(409)을 맞는다. (전 18개 update 서비스 공통 — 여기선 HR Department로 대표 박제.)
 *
 * <p>계약: 지금은 의도적으로 RED(AssertionError) — 서비스가 flush 후 재매핑하도록 고치면 GREEN.
 */
@Transactional
class P0VersionStaleResponseIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DepartmentService departmentService;
  @Autowired private DepartmentRepository departmentRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    authenticate("hr-admin", "hr:department:read", "hr:department:write");
  }

  @Test
  void update_response_returnsIncrementedVersion_notStale() {
    DepartmentResponse created =
        departmentService.create(new DepartmentCreateRequest("DEV", "개발팀", null, 0));

    DepartmentResponse updated =
        departmentService.update(
            created.id(), new DepartmentUpdateRequest("개발1팀", 0, created.version()));

    // 대칭 검증: DB는 실제로 version을 증가시켰다(=DTO만 stale임을 입증). 이 단언은 통과한다.
    departmentRepository.flush();
    entityManager.clear();
    Department reloaded = departmentRepository.findById(created.id()).orElseThrow();
    assertThat(reloaded.getVersion())
        .as("DB 재조회 version은 증가해 있어야 한다")
        .isEqualTo(created.version() + 1);

    // 버그 증명: 갱신 응답 DTO의 version도 증가해 있어야 한다 — 현재는 flush 전 매핑이라 stale(0) → FAIL.
    assertThat(updated.version())
        .as("update 응답 DTO version은 증가 전(stale) 값이면 안 된다")
        .isEqualTo(created.version() + 1);
  }
}
