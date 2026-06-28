package com.erp.common.userdirectory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Keycloak sub → 표시이름·이메일 로컬 미러(표시 전용 캐시). 쓰기는 {@link UserDirectoryRepository#upsert} 네이티브 upsert로만
 * 일어나고, 이 엔티티는 sub→이름 해소(읽기)에 쓴다. {@link com.erp.common.security.Role}와 동일하게 Hibernate 테넌트 필터 대신
 * tenant_id 명시 필터링.
 */
@Entity
@Table(name = "user_directory", schema = "common")
public class UserDirectoryEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_directory_seq")
  @SequenceGenerator(
      name = "user_directory_seq",
      sequenceName = "common.user_directory_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "sub", nullable = false, length = 100)
  private String sub;

  @Column(name = "display_name", nullable = false, length = 200)
  private String displayName;

  @Column(name = "email", length = 320)
  private String email;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected UserDirectoryEntry() {}

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getSub() {
    return sub;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
