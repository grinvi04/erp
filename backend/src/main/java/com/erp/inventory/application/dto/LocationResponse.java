package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;

public record LocationResponse(
    Long id,
    Long warehouseId,
    String warehouseName,
    String code,
    String name,
    Long parentId,
    String parentName,
    LocationType locationType,
    boolean active,
    Long version) {
  public static LocationResponse from(Location loc) {
    Long parentId = loc.getParent() != null ? loc.getParent().getId() : null;
    String parentName = loc.getParent() != null ? loc.getParent().getName() : null;
    return new LocationResponse(
        loc.getId(),
        loc.getWarehouse().getId(),
        loc.getWarehouse().getName(),
        loc.getCode(),
        loc.getName(),
        parentId,
        parentName,
        loc.getLocationType(),
        loc.isActive(),
        loc.getVersion());
  }
}
