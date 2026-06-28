package com.erp.hr.application.dto;

import com.erp.hr.domain.model.EmploymentType;

public record EmploymentTypeCountResponse(EmploymentType employmentType, long count) {}
