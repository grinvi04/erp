package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.LeadStatus;

public interface LeadStatusCountRow {
    LeadStatus getStatus();
    long getCount();
}
