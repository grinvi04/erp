package com.erp.crm.application.dto;

import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String lastName,
        String firstName,
        String company,
        String title,
        String email,
        String phone,
        String source,
        LeadStatus status,
        String ownerId,
        Long convertedAccountId,
        Long convertedOpportunityId,
        LocalDateTime convertedAt,
        String note,
        LocalDateTime createdAt,
        Long version
) {
    public static LeadResponse from(Lead l) {
        return new LeadResponse(l.getId(), l.getLastName(), l.getFirstName(), l.getCompany(),
                l.getTitle(), l.getEmail(), l.getPhone(), l.getSource(), l.getStatus(),
                l.getOwnerId(),
                l.getConvertedAccount() != null ? l.getConvertedAccount().getId() : null,
                l.getConvertedOpportunityId(), l.getConvertedAt(), l.getNote(), l.getCreatedAt(),
                l.getVersion());
    }
}
