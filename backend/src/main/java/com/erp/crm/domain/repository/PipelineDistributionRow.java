package com.erp.crm.domain.repository;

import java.math.BigDecimal;

public interface PipelineDistributionRow {
    Long getStageId();
    String getStageName();
    Integer getStageOrder();
    long getCount();
    BigDecimal getTotalAmount();
}
