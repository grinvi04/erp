package com.erp.hr.application.dto;

import com.erp.hr.domain.model.Position;

public record PositionResponse(Long id, String code, String name, int levelOrder, Long version) {
  public static PositionResponse from(Position position) {
    return new PositionResponse(
        position.getId(),
        position.getCode(),
        position.getName(),
        position.getLevelOrder(),
        position.getVersion());
  }
}
