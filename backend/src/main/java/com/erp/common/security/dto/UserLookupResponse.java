package com.erp.common.security.dto;

/**
 * IAM 사용자 존재 검증 응답. user_directory가 없으므로 IAM이 자체적으로 아는 흔적(감사 기록·역할 배정·접근 프로파일) 중 하나라도 있으면 {@code
 * known=true}로 본다 — 임의로 타이핑한 유령 sub에 무단으로 역할이 배정되는 것을 화면에서 경고·차단하기 위한 신호.
 */
public record UserLookupResponse(
    String userId, boolean known, int roleCount, boolean hasAccessProfile, boolean audited) {}
