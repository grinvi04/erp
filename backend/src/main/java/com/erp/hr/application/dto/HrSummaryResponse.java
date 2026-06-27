package com.erp.hr.application.dto;

public record HrSummaryResponse(
    long activeEmployees, long onLeaveEmployees, long pendingLeaveRequests) {}
