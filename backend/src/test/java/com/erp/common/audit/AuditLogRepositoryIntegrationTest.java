package com.erp.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** 감사 로그 조회 쿼리 — 액션·기간·수행자 필터와 존재 검증 쿼리가 테넌트 격리 하에 동작하는지 검증한다. */
@Transactional
class AuditLogRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private AuditLogRepository repository;

  @BeforeEach
  void seed() {
    repository.save(
        AuditLog.of(
            TEST_TENANT_ID,
            "AP_INVOICE",
            1L,
            AuditLog.AuditAction.APPROVE,
            null,
            null,
            "alice",
            null));
    repository.save(
        AuditLog.of(
            TEST_TENANT_ID,
            "AP_INVOICE",
            2L,
            AuditLog.AuditAction.REJECT,
            null,
            null,
            "bob",
            null));
    // 다른 테넌트 — 격리 검증용.
    repository.save(
        AuditLog.of(
            99L, "AP_INVOICE", 3L, AuditLog.AuditAction.APPROVE, null, null, "carol", null));
  }

  @Test
  void search_byAction_filtersToMatchingActionOnly() {
    var page =
        repository.search(
            TEST_TENANT_ID,
            null,
            null,
            null,
            AuditLog.AuditAction.APPROVE,
            null,
            null,
            PageRequest.of(0, 20));

    assertThat(page.getContent())
        .extracting(AuditLog::getPerformedBy)
        .containsExactly("alice"); // bob(REJECT)·carol(타테넌트) 제외
  }

  @Test
  void search_byPerformer_filtersToThatUser() {
    var page =
        repository.search(
            TEST_TENANT_ID, null, null, "bob", null, null, null, PageRequest.of(0, 20));

    assertThat(page.getContent()).extracting(AuditLog::getPerformedBy).containsExactly("bob");
  }

  @Test
  void search_byDateRange_excludesFutureFrom() {
    LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
    var page =
        repository.search(
            TEST_TENANT_ID, null, null, null, null, tomorrow, null, PageRequest.of(0, 20));

    assertThat(page.getContent()).isEmpty();
  }

  @Test
  void search_byDateRange_includesWithinWindow() {
    LocalDateTime from = LocalDateTime.now().minusHours(1);
    LocalDateTime to = LocalDateTime.now().plusHours(1);
    var page =
        repository.search(TEST_TENANT_ID, null, null, null, null, from, to, PageRequest.of(0, 20));

    assertThat(page.getContent()).hasSize(2); // alice·bob (carol는 타테넌트)
  }

  @Test
  void existsByPerformedBy_isTenantScoped() {
    assertThat(repository.existsByTenantIdAndPerformedBy(TEST_TENANT_ID, "alice")).isTrue();
    assertThat(repository.existsByTenantIdAndPerformedBy(TEST_TENANT_ID, "ghost")).isFalse();
    // carol는 타테넌트 — 현재 테넌트에서는 존재하지 않아야 한다.
    assertThat(repository.existsByTenantIdAndPerformedBy(TEST_TENANT_ID, "carol")).isFalse();
  }
}
