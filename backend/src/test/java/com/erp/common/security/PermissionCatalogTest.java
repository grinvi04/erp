package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Permission.all() 카탈로그가 선언된 권한 코드 상수와 정확히 일치하는지 리플렉션으로 검증한다. 상수를 추가하고 all()에 빠뜨리면(또는 그 반대) 실패 —
 * 관리화면 선택지·부트스트랩 누락 방지.
 */
class PermissionCatalogTest {

  @Test
  void all_matchesEveryDeclaredPermissionConstant() throws IllegalAccessException {
    Set<String> declared = new HashSet<>();
    for (Field f : Permission.class.getDeclaredFields()) {
      if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
        declared.add((String) f.get(null));
      }
    }
    assertThat(Permission.all()).containsExactlyInAnyOrderElementsOf(declared);
  }
}
