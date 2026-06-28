package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.LeavePolicy.LeaveType;
import java.math.BigDecimal;

public interface LeaveTypeStatRow {
  LeaveType getLeaveType();

  long getCount();

  BigDecimal getTotalDays();
}
