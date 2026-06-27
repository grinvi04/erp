package com.erp.hr.application.dto;

import com.erp.hr.domain.model.LeavePolicy.LeaveType;
import java.math.BigDecimal;

public record LeaveTypeStatResponse(LeaveType leaveType, long count, BigDecimal totalDays) {}
