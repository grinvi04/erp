package com.erp.common.security;

/**
 * 데이터 스코프 정책 — 기능 권한(RBAC)과 직교하는 "어느 범위의 데이터를 보나"(auth-standards). ALL=전사, DEPARTMENT=자기 부서+하위,
 * SELF=본인 데이터만. 사용자별 정책은 JWT data_scope 클레임으로 전달되며, 미설정 시 ALL(narrowing 없음 — 1차 통제는 기능 권한이 담당).
 */
public enum DataScope {
  ALL,
  DEPARTMENT,
  SELF
}
