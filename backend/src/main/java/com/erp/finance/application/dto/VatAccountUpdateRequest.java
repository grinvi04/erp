package com.erp.finance.application.dto;

/** 부가세 통제계정 설정 변경 — 부가세대급금(매입)·부가세예수금(매출) 계정 ID. 둘 다 nullable(미지정 시 해제 → 부가세 분개 폴백). */
public record VatAccountUpdateRequest(Long vatReceivableAccountId, Long vatPayableAccountId) {}
