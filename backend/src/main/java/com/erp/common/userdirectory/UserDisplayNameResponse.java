package com.erp.common.userdirectory;

/** sub → 표시이름 해소 응답. UUID(sub)를 사람 이름으로 바꾸는 표시 전용. */
public record UserDisplayNameResponse(String sub, String displayName) {}
