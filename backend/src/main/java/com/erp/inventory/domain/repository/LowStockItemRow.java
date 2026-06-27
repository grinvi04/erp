package com.erp.inventory.domain.repository;

import java.math.BigDecimal;

public interface LowStockItemRow {
  String getSku();

  String getName();

  String getCategoryName();

  BigDecimal getCurrentQty();

  BigDecimal getReorderPoint();
}
