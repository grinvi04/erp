package com.erp.crm.domain.repository;

import java.math.BigDecimal;

public interface PipelineDistributionRow {
  Long getStageId();

  String getStageName();

  Integer getStageOrder();

  String getCurrency();

  long getCount();

  BigDecimal getTotalAmount();

  // 이 단계·통화 그룹의 기준통화 환산액(base_amount) 합계. 산정된 행이 없으면 null.
  BigDecimal getBaseTotal();
}
