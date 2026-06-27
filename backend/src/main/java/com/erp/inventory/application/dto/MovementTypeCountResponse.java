package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.MovementType;

public record MovementTypeCountResponse(MovementType movementType, long count) {}
