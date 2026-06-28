package com.erp.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.PermissionChecker;
import com.erp.common.tenant.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private PermissionChecker permissionChecker;

  private AuditService auditService;

  @BeforeEach
  void setUp() {
    auditService = new AuditService(auditLogRepository, currentUserProvider, permissionChecker);
    TenantContext.setTenantId(1L);
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  private AuditLog logForTenant(Long tenantId) {
    return AuditLog.of(
        tenantId,
        "LEAVE_REQUEST",
        10L,
        AuditLog.AuditAction.UPDATE,
        "{\"status\":\"DRAFT\"}",
        "{\"status\":\"APPROVED\"}",
        "MANAGER",
        "10.0.0.1");
  }

  @Test
  void findById_sameTenant_returnsDetailWithBeforeAfter() {
    given(auditLogRepository.findById(5L)).willReturn(Optional.of(logForTenant(1L)));

    AuditLogDetailResponse detail = auditService.findById(5L);

    assertThat(detail.beforeData()).isEqualTo("{\"status\":\"DRAFT\"}");
    assertThat(detail.afterData()).isEqualTo("{\"status\":\"APPROVED\"}");
  }

  @Test
  void findById_otherTenant_isTreatedAsNotFound() {
    // 다른 테넌트의 로그는 존재해도 조회되지 않아야 한다(테넌트 격리).
    given(auditLogRepository.findById(5L)).willReturn(Optional.of(logForTenant(2L)));

    assertThatThrownBy(() -> auditService.findById(5L))
        .isInstanceOf(ErpException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }
}
