package com.erp.crm.application.dto;

import com.erp.crm.domain.model.Contact;
import java.time.LocalDateTime;

public record ContactResponse(
    Long id,
    Long accountId,
    String accountName,
    String lastName,
    String firstName,
    String title,
    String department,
    String email,
    String phone,
    String mobile,
    boolean isPrimary,
    LocalDateTime createdAt,
    Long version) {
  public static ContactResponse from(Contact c) {
    return new ContactResponse(
        c.getId(),
        c.getAccount().getId(),
        c.getAccount().getName(),
        c.getLastName(),
        c.getFirstName(),
        c.getTitle(),
        c.getDepartment(),
        c.getEmail(),
        c.getPhone(),
        c.getMobile(),
        c.isPrimary(),
        c.getCreatedAt(),
        c.getVersion());
  }
}
