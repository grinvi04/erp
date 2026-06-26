package com.erp.crm.application.service;

import com.erp.common.response.CurrencyAmount;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.CrmSummaryResponse;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.ActivityRepository;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.util.List;
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
    private final CrmDataScopeResolver dataScopeResolver;
    private final PermissionChecker permissionChecker;

    public CrmSummaryResponse getSummary() {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();
        List<CurrencyAmount> openAmounts =
                opportunityRepository.sumOpenAmountByCurrency(s.scoped(), s.ownerIds());
        return new CrmSummaryResponse(
                opportunityRepository.countOpen(s.scoped(), s.ownerIds()),
                openAmounts,
                leadRepository.countByStatus(LeadStatus.NEW, s.scoped(), s.ownerIds()),
                activityRepository.countByStatus(ActivityStatus.OPEN, s.scoped(), s.ownerIds()));
    }
}
