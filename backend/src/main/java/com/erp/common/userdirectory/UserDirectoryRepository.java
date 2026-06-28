package com.erp.common.userdirectory;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDirectoryRepository extends JpaRepository<UserDirectoryEntry, Long> {

  List<UserDirectoryEntry> findByTenantIdAndSubIn(Long tenantId, Collection<String> subs);

  /**
   * (tenant_id, sub) upsert — 표시이름·이메일이 실제로 바뀐 경우에만 UPDATE(IS DISTINCT FROM)해 요청마다 불필요한 쓰기를 피한다.
   * tenant_id를 행에 명시 보관해 테넌트 간 격리를 DB 레벨에서 보장한다.
   */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO common.user_directory (tenant_id, sub, display_name, email, updated_at)
          VALUES (:tenantId, :sub, :displayName, :email, now())
          ON CONFLICT (tenant_id, sub) DO UPDATE
             SET display_name = EXCLUDED.display_name,
                 email        = EXCLUDED.email,
                 updated_at   = now()
           WHERE common.user_directory.display_name IS DISTINCT FROM EXCLUDED.display_name
              OR common.user_directory.email        IS DISTINCT FROM EXCLUDED.email
          """,
      nativeQuery = true)
  void upsert(
      @Param("tenantId") Long tenantId,
      @Param("sub") String sub,
      @Param("displayName") String displayName,
      @Param("email") String email);
}
