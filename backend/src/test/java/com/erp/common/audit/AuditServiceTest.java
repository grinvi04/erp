package com.erp.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.observability.TraceIdFilter;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.PermissionChecker;
import com.erp.common.tenant.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

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
    MDC.clear();
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
        "10.0.0.1",
        "fedcba9876543210fedcba9876543210");
  }

  @Test
  void findById_sameTenant_returnsDetailWithBeforeAfter() {
    given(auditLogRepository.findById(5L)).willReturn(Optional.of(logForTenant(1L)));

    AuditLogDetailResponse detail = auditService.findById(5L);

    assertThat(detail.beforeData()).isEqualTo("{\"status\":\"DRAFT\"}");
    assertThat(detail.afterData()).isEqualTo("{\"status\":\"APPROVED\"}");
    assertThat(detail.traceId()).isEqualTo("fedcba9876543210fedcba9876543210");
  }

  @Test
  void findById_otherTenant_isTreatedAsNotFound() {
    // 다른 테넌트의 로그는 존재해도 조회되지 않아야 한다(테넌트 격리).
    given(auditLogRepository.findById(5L)).willReturn(Optional.of(logForTenant(2L)));

    assertThatThrownBy(() -> auditService.findById(5L))
        .isInstanceOf(ErpException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void record_storesCurrentRequestTraceId() {
    given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
    MDC.put(TraceIdFilter.MDC_TRACE_ID, "0123456789abcdef0123456789abcdef");

    auditService.record("ROLE", 7L, AuditLog.AuditAction.CREATE, null, "{\"name\":\"admin\"}");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
  }

  @Test
  void record_withoutRequestTraceId_storesNull() {
    given(currentUserProvider.getCurrentUserId()).willReturn("system-job");

    auditService.record("TENANT", 7L, AuditLog.AuditAction.UPDATE, null, null);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getTraceId()).isNull();
  }
}
