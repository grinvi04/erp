package com.erp.crm.application.service;

import com.erp.crm.application.dto.CrmSummaryResponse;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.ActivityRepository;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmSummaryService {

    private final OpportunityRepository opportunityRepository;
    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;

    public CrmSummaryResponse getSummary() {
        BigDecimal openAmount = opportunityRepository.sumOpenAmount();
        return new CrmSummaryResponse(
                opportunityRepository.countOpen(),
                openAmount != null ? openAmount : BigDecimal.ZERO,
                leadRepository.countByStatus(LeadStatus.NEW),
                activityRepository.countByStatus(ActivityStatus.OPEN));
    }
}
