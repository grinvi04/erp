package com.erp.crm.application.dto;

import com.erp.crm.domain.model.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ActivityCreateRequest(
    @NotNull ActivityType activityType,
    @NotBlank @Size(max = 300) String subject,
    Long accountId,
    Long contactId,
    Long opportunityId,
    LocalDateTime dueDate,
    String description) {}
