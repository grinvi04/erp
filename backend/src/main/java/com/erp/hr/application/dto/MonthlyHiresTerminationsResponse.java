package com.erp.hr.application.dto;

public record MonthlyHiresTerminationsResponse(
        int month,
        long hires,
        long terminations
) {
}
