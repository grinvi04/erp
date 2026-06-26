package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.EmploymentType;

public interface EmploymentTypeCountRow {
    EmploymentType getEmploymentType();
    long getCount();
}
