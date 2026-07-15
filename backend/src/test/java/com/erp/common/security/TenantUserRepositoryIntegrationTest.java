package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.tenant.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TenantUserRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantUserRepository repository;

  @Test
  void lookupIsTenantScopedAndAllowsSameEmailInAnotherTenant() {
    String email = uniqueEmail();
    TenantUser tenantOne = repository.saveAndFlush(TenantUser.pending(email, uniqueKey()));

    TenantContext.setTenantId(2L);
    assertThat(repository.findByNormalizedEmail(email)).isEmpty();
    TenantUser tenantTwo = repository.saveAndFlush(TenantUser.pending(email, uniqueKey()));

    assertThat(tenantTwo.getId()).isNotEqualTo(tenantOne.getId());
    assertThat(repository.findByNormalizedEmail(email)).isPresent();
  }

  @Test
  void normalizedEmailAndRequestKeyAreUniqueWithinTenant() {
    String email = uniqueEmail();
    String requestKey = uniqueKey();
    repository.saveAndFlush(TenantUser.pending(email, requestKey));

    assertThatThrownBy(
            () -> repository.saveAndFlush(TenantUser.pending(email.toUpperCase(), uniqueKey())))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> repository.saveAndFlush(TenantUser.pending(uniqueEmail(), requestKey)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static String uniqueEmail() {
    return UUID.randomUUID() + "@example.com";
  }

  private static String uniqueKey() {
    return UUID.randomUUID().toString();
  }
}
