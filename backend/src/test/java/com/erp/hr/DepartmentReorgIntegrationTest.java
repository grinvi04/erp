package com.erp.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.dto.DepartmentUpdateRequest;
import com.erp.hr.application.service.DepartmentService;
import com.erp.hr.domain.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서 재편성(T2-7) — 수정 시 상위부서 변경·활성 토글, 순환 부서 방지.
 *
 * <p>버그: 생성 후 부모 이동·폐지(비활성)가 불가했다. 수정 유스케이스가 parent 변경·active 토글을 지원하고, 자기 자신/자기 하위를 부모로 지정하는 순환을
 * 거부하는지 검증한다.
 */
@Transactional
class DepartmentReorgIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DepartmentService departmentService;
  @Autowired private DepartmentRepository departmentRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    authenticate("hr-admin", Permission.HR_DEPARTMENT_READ, Permission.HR_DEPARTMENT_WRITE);
  }

  private DepartmentResponse create(String code, String name, Long parentId) {
    return departmentService.create(new DepartmentCreateRequest(code, name, parentId, 0));
  }

  private DepartmentUpdateRequest reparent(DepartmentResponse dept, Long parentId) {
    return new DepartmentUpdateRequest(
        dept.name(), dept.sortOrder(), parentId, dept.active(), dept.version());
  }

  @Test
  void update_changeParent_movesDepartmentUnderNewParent() {
    DepartmentResponse parent = create("A", "사업본부", null);
    DepartmentResponse target = create("B", "독립팀", null);

    DepartmentResponse moved = departmentService.update(target.id(), reparent(target, parent.id()));

    assertThat(moved.parentId()).isEqualTo(parent.id());
    assertThat(moved.depth()).isEqualTo(1);
  }

  @Test
  void update_changeParentToRoot_clearsParent() {
    DepartmentResponse parent = create("A", "사업본부", null);
    DepartmentResponse child = create("B", "하위팀", parent.id());

    DepartmentResponse moved = departmentService.update(child.id(), reparent(child, null));

    assertThat(moved.parentId()).isNull();
    assertThat(moved.depth()).isZero();
  }

  @Test
  void update_selfAsParent_throwsDepartmentCycle() {
    DepartmentResponse dept = create("A", "사업본부", null);

    assertThatThrownBy(() -> departmentService.update(dept.id(), reparent(dept, dept.id())))
        .isInstanceOf(ErpException.class)
        .extracting(e -> ((ErpException) e).getErrorCode())
        .isEqualTo(ErrorCode.DEPARTMENT_CYCLE);
  }

  @Test
  void update_descendantAsParent_throwsDepartmentCycle() {
    DepartmentResponse a = create("A", "본부", null);
    DepartmentResponse b = create("B", "팀", a.id());

    // A(상위)를 B(자기 하위)의 하위로 이동 → 순환 → 거부
    assertThatThrownBy(() -> departmentService.update(a.id(), reparent(a, b.id())))
        .isInstanceOf(ErpException.class)
        .extracting(e -> ((ErpException) e).getErrorCode())
        .isEqualTo(ErrorCode.DEPARTMENT_CYCLE);
  }

  @Test
  void update_movingSubtree_recomputesDescendantDepth() {
    DepartmentResponse a = create("A", "본부", null);
    DepartmentResponse b = create("B", "팀", a.id());
    DepartmentResponse c = create("C", "파트", b.id());
    assertThat(c.depth()).isEqualTo(2);

    // B를 루트로 이동 → B.depth=0, 하위 C.depth=1 로 갱신되어야 한다
    departmentService.update(b.id(), reparent(b, null));
    departmentRepository.flush();
    entityManager.clear();

    assertThat(departmentRepository.findById(b.id()).orElseThrow().getDepth()).isZero();
    assertThat(departmentRepository.findById(c.id()).orElseThrow().getDepth()).isEqualTo(1);
  }

  @Test
  void update_toggleActive_deactivatesAndReactivates() {
    DepartmentResponse dept = create("A", "사업본부", null);
    assertThat(dept.active()).isTrue();

    DepartmentResponse deactivated =
        departmentService.update(
            dept.id(),
            new DepartmentUpdateRequest(
                dept.name(), dept.sortOrder(), null, false, dept.version()));
    assertThat(deactivated.active()).isFalse();

    DepartmentResponse reactivated =
        departmentService.update(
            dept.id(),
            new DepartmentUpdateRequest(
                deactivated.name(), deactivated.sortOrder(), null, true, deactivated.version()));
    assertThat(reactivated.active()).isTrue();
  }
}
