package com.erp.hr.application.dto;

import com.erp.hr.domain.model.EmployeeStatus;

public record EmployeeStatusCountResponse(EmployeeStatus status, long count) {}
