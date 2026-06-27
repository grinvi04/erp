package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.MovementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record MovementCreateRequest(
    @NotNull MovementType movementType,
    @NotNull LocalDate movementDate,
    @Size(max = 100) String referenceType,
    Long referenceId,
    String note,
    @NotEmpty @Valid List<MovementLineRequest> lines) {}
