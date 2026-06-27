package com.erp.finance.application.dto;

import jakarta.validation.constraints.NotBlank;

/** GL 전표 결재 반려 요청 — 반려 사유(comment) 필수. */
public record JournalEntryRejectRequest(
    @NotBlank String comment
) {}
