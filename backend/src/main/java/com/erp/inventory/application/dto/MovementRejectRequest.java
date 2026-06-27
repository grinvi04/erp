package com.erp.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 재고 조정 이동 결재 반려 요청 — 반려 사유(comment) 필수. */
public record MovementRejectRequest(@NotBlank String comment) {}
