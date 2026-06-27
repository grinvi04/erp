package com.erp.crm.application.dto;

import com.erp.crm.domain.model.LeadStatus;

public record LeadStatusCountResponse(LeadStatus status, long count) {}
