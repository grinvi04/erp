package com.erp.finance.application.dto;

import com.erp.finance.domain.model.DepreciationMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetCreateRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 200) String name,
    @NotNull LocalDate acquisitionDate,
    // NUMERIC(20,2) 컬럼 — 소수 2자리 초과 입력은 400으로 거부(엔티티 setScale 예외/오버플로 방지).
    @NotNull @DecimalMin("0.01") @Digits(integer = 18, fraction = 2) BigDecimal acquisitionCost,
    @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal residualValue,
    @Positive int usefulLifeMonths,
    @NotNull DepreciationMethod method,
    // 정률법일 때만 필요(연상각률, 예 0.45). NUMERIC(6,4) 컬럼 한도 내로 검증(오버플로 → 500 방지).
    @DecimalMin("0") @Digits(integer = 2, fraction = 4) BigDecimal decliningAnnualRate,
    @NotNull Long assetAccountId) {}
