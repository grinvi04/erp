package com.erp.inventory.domain.repository;

import java.math.BigDecimal;

public interface WarehouseStockRow {
  Long getWarehouseId();

  String getWarehouseName();

  BigDecimal getTotalQty();

  BigDecimal getTotalValue();
}
