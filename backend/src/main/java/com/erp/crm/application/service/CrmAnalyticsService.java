package com.erp.crm.application.service;

import com.erp.crm.application.dto.LeadStatusCountResponse;
import com.erp.crm.application.dto.PipelineDistributionResponse;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.LeadStatusCountRow;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmAnalyticsService {

    private final PipelineStageRepository pipelineStageRepository;
    private final LeadRepository leadRepository;

    public List<PipelineDistributionResponse> getPipelineDistribution() {
        return pipelineStageRepository.pipelineDistribution().stream()
                .map(r -> new PipelineDistributionResponse(
                        r.getStageId(),
                        r.getStageName(),
                        r.getStageOrder(),
                        r.getCount(),
                        r.getTotalAmount()))
                .collect(Collectors.toList());
    }

    public List<LeadStatusCountResponse> getLeadsByStatus() {
        Map<LeadStatus, Long> countMap = leadRepository.countByStatusGrouped().stream()
                .collect(Collectors.toMap(LeadStatusCountRow::getStatus, LeadStatusCountRow::getCount));

        return Arrays.stream(LeadStatus.values())
                .map(status -> new LeadStatusCountResponse(status, countMap.getOrDefault(status, 0L)))
                .collect(Collectors.toList());
    }
}
