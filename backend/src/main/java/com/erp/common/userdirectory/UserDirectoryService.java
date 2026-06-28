package com.erp.common.userdirectory;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 sub→표시이름 미러 관리. {@code sync}는 인증 요청마다 현재 사용자를 누적(upsert)하고, {@code displayNames}는 sub 집합을 사람
 * 이름으로 해소한다. 테넌트 격리는 호출자가 넘긴 tenantId로 명시 적용한다.
 */
@Service
@RequiredArgsConstructor
public class UserDirectoryService {

  private final UserDirectoryRepository repository;

  @Transactional
  public void sync(Long tenantId, String sub, String displayName, String email) {
    if (tenantId == null || sub == null || sub.isBlank()) {
      return;
    }
    String name = (displayName != null && !displayName.isBlank()) ? displayName : sub;
    repository.upsert(tenantId, sub, name, email);
  }

  /** sub 집합 → 표시이름 맵. 미존재 sub는 결과에서 누락된다(호출자가 fallback 처리). */
  @Transactional(readOnly = true)
  public Map<String, String> displayNames(Long tenantId, Collection<String> subs) {
    if (tenantId == null || subs == null || subs.isEmpty()) {
      return Map.of();
    }
    return repository.findByTenantIdAndSubIn(tenantId, subs).stream()
        .collect(Collectors.toMap(UserDirectoryEntry::getSub, UserDirectoryEntry::getDisplayName));
  }
}
