package com.erp.common.userdirectory;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** user_directory 미러의 upsert·해소·테넌트 격리를 검증한다 — JWT 클레임으로 누적한 sub→이름을 표시 시 되돌려준다. */
@Transactional
class UserDirectoryServiceIntegrationTest extends AbstractIntegrationTest {

  private static final Long OTHER_TENANT_ID = 2L;

  @Autowired private UserDirectoryService userDirectoryService;

  @Test
  void sync_thenResolve_returnsDisplayName() {
    userDirectoryService.sync(TEST_TENANT_ID, "sub-alice", "Alice Kim", "alice@erp.com");

    Map<String, String> names =
        userDirectoryService.displayNames(TEST_TENANT_ID, List.of("sub-alice"));

    assertThat(names).containsEntry("sub-alice", "Alice Kim");
  }

  @Test
  void sync_isUpsert_updatesNameOnSecondCall() {
    userDirectoryService.sync(TEST_TENANT_ID, "sub-bob", "Bob", "bob@erp.com");
    userDirectoryService.sync(TEST_TENANT_ID, "sub-bob", "Bob Lee", "bob.lee@erp.com");

    assertThat(userDirectoryService.displayNames(TEST_TENANT_ID, List.of("sub-bob")))
        .containsEntry("sub-bob", "Bob Lee");
  }

  @Test
  void resolve_unknownSub_isOmitted() {
    userDirectoryService.sync(TEST_TENANT_ID, "sub-known", "Known", null);

    Map<String, String> names =
        userDirectoryService.displayNames(TEST_TENANT_ID, List.of("sub-known", "sub-missing"));

    assertThat(names).containsEntry("sub-known", "Known").doesNotContainKey("sub-missing");
  }

  @Test
  void blankDisplayName_fallsBackToSub() {
    userDirectoryService.sync(TEST_TENANT_ID, "sub-noname", "  ", null);

    assertThat(userDirectoryService.displayNames(TEST_TENANT_ID, List.of("sub-noname")))
        .containsEntry("sub-noname", "sub-noname");
  }

  @Test
  void resolve_isTenantIsolated() {
    userDirectoryService.sync(TEST_TENANT_ID, "sub-shared", "Tenant1 Name", null);
    userDirectoryService.sync(OTHER_TENANT_ID, "sub-shared", "Tenant2 Name", null);

    assertThat(userDirectoryService.displayNames(TEST_TENANT_ID, List.of("sub-shared")))
        .containsEntry("sub-shared", "Tenant1 Name");
    assertThat(userDirectoryService.displayNames(OTHER_TENANT_ID, List.of("sub-shared")))
        .containsEntry("sub-shared", "Tenant2 Name");
  }
}
