package com.erp.finance.application.dto;

import com.erp.finance.domain.model.JournalEntryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record JournalEntryCreateRequest(
    @NotNull LocalDate entryDate,
    @NotNull Long fiscalPeriodId,
    @NotBlank @Size(max = 500) String description,
    @NotNull JournalEntryType entryType,
    String currency,
    @NotEmpty @Valid List<JournalLineRequest> lines) {}
