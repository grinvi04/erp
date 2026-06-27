package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.Stock;
import java.math.BigDecimal;

public record StockResponse(
    Long id,
    Long itemId,
    String itemSku,
    String itemName,
    Long locationId,
    String locationCode,
    String locationName,
    Long warehouseId,
    String warehouseName,
    String lotNo,
    String serialNo,
    BigDecimal qtyOnHand,
    BigDecimal qtyReserved,
    BigDecimal unitCost) {
  public static StockResponse from(Stock s) {
    return new StockResponse(
        s.getId(),
        s.getItem().getId(),
        s.getItem().getSku(),
        s.getItem().getName(),
        s.getLocation().getId(),
        s.getLocation().getCode(),
        s.getLocation().getName(),
        s.getLocation().getWarehouse().getId(),
        s.getLocation().getWarehouse().getName(),
        s.getLotNo(),
        s.getSerialNo(),
        s.getQtyOnHand(),
        s.getQtyReserved(),
        s.getUnitCost());
  }
}
