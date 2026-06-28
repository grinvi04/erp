package com.erp.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.service.DepartmentService;
import com.erp.hr.domain.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 버그2 재현(RED) — 소프트삭제(deleted_at) 행이 목록에 그대로 노출된다.
 *
 * <p>{@code @SQLRestriction("deleted_at IS NULL")}이 {@code @MappedSuperclass}(BaseEntity)에 선언돼 있어
 * 하위 {@code @Entity}로 상속되지 않는다 → 소프트삭제 필터가 실제 SQL에 안 붙어 목록·조회에 삭제 행이 남는다. (deleted_at 세팅형 엔티티 공통 —
 * 여기선 HR Department로 대표 박제. Department.delete()는 softDelete()로 deleted_at을 세팅한다.)
 *
 * <p>계약: 지금은 의도적으로 RED(AssertionError) — @SQLRestriction을 하위 @Entity로 내리면 GREEN.
 */
@Transactional
class P0SoftDeleteFilterIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DepartmentService departmentService;
  @Autowired private DepartmentRepository departmentRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    authenticate("hr-admin", "hr:department:read", "hr:department:write");
  }

  @Test
  void softDeleted_excludedFromList() {
    DepartmentResponse created =
        departmentService.create(new DepartmentCreateRequest("DEV", "개발팀", null, 0));

    departmentService.delete(created.id()); // deleted_at 세팅(소프트삭제)

    // L1 캐시 회피 — DB 왕복을 강제해 @SQLRestriction이 실제 SELECT SQL에 적용되는지 본다.
    departmentRepository.flush();
    entityManager.clear();

    List<Long> ids = departmentService.findAll().stream().map(DepartmentResponse::id).toList();

    // 버그 증명: 소프트삭제한 id는 목록에서 빠져야 한다 — 현재는 필터 미적용으로 남아 있어 FAIL.
    assertThat(ids).as("소프트삭제(deleted_at)된 부서는 목록 조회에서 제외되어야 한다").doesNotContain(created.id());
  }
}
