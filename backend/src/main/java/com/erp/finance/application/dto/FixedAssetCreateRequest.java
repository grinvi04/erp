package com.erp.finance.application.dto;

import com.erp.finance.domain.model.DepreciationMethod;
import jakarta.validation.constraints.DecimalMin;
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
    @NotNull @DecimalMin("0.01") BigDecimal acquisitionCost,
    @NotNull @DecimalMin("0") BigDecimal residualValue,
    @Positive int usefulLifeMonths,
    @NotNull DepreciationMethod method,
    // 정률법일 때만 필요(연상각률, 예 0.45).
    BigDecimal decliningAnnualRate,
    @NotNull Long assetAccountId) {}
