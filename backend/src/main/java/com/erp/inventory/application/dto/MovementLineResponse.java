package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.MovementLine;
import java.math.BigDecimal;

public record MovementLineResponse(
    Long id,
    Integer lineNo,
    Long itemId,
    String itemSku,
    String itemName,
    Long fromLocationId,
    String fromLocationCode,
    Long toLocationId,
    String toLocationCode,
    String lotNo,
    String serialNo,
    BigDecimal qty,
    BigDecimal unitCost) {
  public static MovementLineResponse from(MovementLine line) {
    Long fromLocId = line.getFromLocation() != null ? line.getFromLocation().getId() : null;
    String fromLocCode = line.getFromLocation() != null ? line.getFromLocation().getCode() : null;
    Long toLocId = line.getToLocation() != null ? line.getToLocation().getId() : null;
    String toLocCode = line.getToLocation() != null ? line.getToLocation().getCode() : null;
    return new MovementLineResponse(
        line.getId(),
        line.getLineNo(),
        line.getItem().getId(),
        line.getItem().getSku(),
        line.getItem().getName(),
        fromLocId,
        fromLocCode,
        toLocId,
        toLocCode,
        line.getLotNo(),
        line.getSerialNo(),
        line.getQty(),
        line.getUnitCost());
  }
}
