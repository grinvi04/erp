package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.tenant.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantUserRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantUserRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

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

  @Test
  void keycloakIdentityIsUniqueWithinTenant() {
    String keycloakUserId = "keycloak-" + uniqueKey();
    TenantUser first = TenantUser.pending(uniqueEmail(), uniqueKey());
    first.activate(keycloakUserId);
    repository.saveAndFlush(first);

    TenantUser duplicate = TenantUser.pending(uniqueEmail(), uniqueKey());
    duplicate.activate(keycloakUserId);

    assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deleteSoftDeletesTenantUserAndExcludesItFromRepositoryQueries() {
    TenantUser user = repository.saveAndFlush(TenantUser.pending(uniqueEmail(), uniqueKey()));

    repository.delete(user);
    repository.flush();

    assertThat(repository.findById(user.getId())).isEmpty();
    assertThat(repository.findAllByOrderByNormalizedEmailAsc())
        .extracting(TenantUser::getId)
        .doesNotContain(user.getId());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM common.tenant_user WHERE id = ?", Long.class, user.getId()))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM common.tenant_user WHERE id = ?",
                Boolean.class,
                user.getId()))
        .isTrue();
  }

  @Test
  void softDeletedTenantUserUniqueValuesCanBeReused() {
    String email = uniqueEmail();
    String requestKey = uniqueKey();
    String keycloakUserId = "keycloak-" + uniqueKey();
    TenantUser deleted = TenantUser.pending(email, requestKey);
    deleted.activate(keycloakUserId);
    deleted = repository.saveAndFlush(deleted);

    repository.delete(deleted);
    repository.flush();

    TenantUser replacement = TenantUser.pending(email, requestKey);
    replacement.activate(keycloakUserId);
    replacement = repository.saveAndFlush(replacement);

    assertThat(replacement.getId()).isNotEqualTo(deleted.getId());
  }

  private static String uniqueEmail() {
    return UUID.randomUUID() + "@example.com";
  }

  private static String uniqueKey() {
    return UUID.randomUUID().toString();
  }
}
