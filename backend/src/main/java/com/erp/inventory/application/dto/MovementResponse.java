package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import java.time.LocalDate;
import java.util.List;

public record MovementResponse(
    Long id,
    String movementNo,
    MovementType movementType,
    MovementStatus status,
    String referenceType,
    Long referenceId,
    LocalDate movementDate,
    String note,
    List<MovementLineResponse> lines) {
  public static MovementResponse from(Movement m) {
    return new MovementResponse(
        m.getId(),
        m.getMovementNo(),
        m.getMovementType(),
        m.getStatus(),
        m.getReferenceType(),
        m.getReferenceId(),
        m.getMovementDate(),
        m.getNote(),
        null);
  }

  public static MovementResponse from(Movement m, List<MovementLineResponse> lines) {
    return new MovementResponse(
        m.getId(),
        m.getMovementNo(),
        m.getMovementType(),
        m.getStatus(),
        m.getReferenceType(),
        m.getReferenceId(),
        m.getMovementDate(),
        m.getNote(),
        lines);
  }
}
