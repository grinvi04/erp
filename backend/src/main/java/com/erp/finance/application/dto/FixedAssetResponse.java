package com.erp.finance.application.dto;

import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.FixedAssetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetResponse(
    Long id,
    String code,
    String name,
    LocalDate acquisitionDate,
    BigDecimal acquisitionCost,
    BigDecimal residualValue,
    int usefulLifeMonths,
    DepreciationMethod method,
    BigDecimal decliningAnnualRate,
    Long assetAccountId,
    BigDecimal accumulatedDepreciation,
    BigDecimal accumulatedImpairment,
    BigDecimal bookValue,
    FixedAssetStatus status) {

  public static FixedAssetResponse from(FixedAsset a) {
    return new FixedAssetResponse(
        a.getId(),
        a.getCode(),
        a.getName(),
        a.getAcquisitionDate(),
        a.getAcquisitionCost(),
        a.getResidualValue(),
        a.getUsefulLifeMonths(),
        a.getMethod(),
        a.getDecliningAnnualRate(),
        a.getAssetAccount() != null ? a.getAssetAccount().getId() : null,
        a.getAccumulatedDepreciation(),
        a.getAccumulatedImpairment(),
        a.bookValue(),
        a.getStatus());
  }
}
