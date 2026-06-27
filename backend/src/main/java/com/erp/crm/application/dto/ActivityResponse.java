package com.erp.crm.application.dto;

import com.erp.crm.domain.model.Activity;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
import java.time.LocalDateTime;

public record ActivityResponse(
    Long id,
    ActivityType activityType,
    String subject,
    Long accountId,
    String accountName,
    Long contactId,
    String contactName,
    Long opportunityId,
    String opportunityName,
    String ownerId,
    LocalDateTime dueDate,
    LocalDateTime completedAt,
    ActivityStatus status,
    String description,
    LocalDateTime createdAt) {
  public static ActivityResponse from(Activity a) {
    return new ActivityResponse(
        a.getId(),
        a.getActivityType(),
        a.getSubject(),
        a.getAccount() != null ? a.getAccount().getId() : null,
        a.getAccount() != null ? a.getAccount().getName() : null,
        a.getContact() != null ? a.getContact().getId() : null,
        a.getContact() != null
            ? a.getContact().getLastName() + " " + a.getContact().getFirstName()
            : null,
        a.getOpportunity() != null ? a.getOpportunity().getId() : null,
        a.getOpportunity() != null ? a.getOpportunity().getName() : null,
        a.getOwnerId(),
        a.getDueDate(),
        a.getCompletedAt(),
        a.getStatus(),
        a.getDescription(),
        a.getCreatedAt());
  }
}
