package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.MovementType;

public interface MovementTypeCountRow {
  MovementType getMovementType();

  long getCount();
}
