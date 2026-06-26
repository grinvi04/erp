package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.EmployeeStatus;

public interface EmployeeStatusCountRow {
    EmployeeStatus getStatus();
    long getCount();
}
