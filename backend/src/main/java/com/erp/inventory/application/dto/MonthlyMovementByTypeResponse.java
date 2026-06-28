package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.MovementType;
import java.util.List;

public record MonthlyMovementByTypeResponse(
    MovementType movementType, List<MonthlyMovementResponse> months) {}
