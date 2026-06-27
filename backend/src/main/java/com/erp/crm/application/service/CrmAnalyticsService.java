package com.erp.crm.application.service;

import com.erp.common.currency.CurrencyConversionPort;
import com.erp.common.response.CurrencyAmount;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.LeadStatusCountResponse;
import com.erp.crm.application.dto.PipelineAnalyticsResponse;
import com.erp.crm.application.dto.PipelineDistributionResponse;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.LeadStatusCountRow;
import com.erp.crm.domain.repository.PipelineDistributionRow;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
    private final CrmDataScopeResolver dataScopeResolver;
    private final CurrencyConversionPort currencyConversionPort;
    private final PermissionChecker permissionChecker;

    public PipelineAnalyticsResponse getPipelineDistribution() {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();

        // 쿼리는 stageOrder, currency 순. 단계별로 통화 행을 묶는다. LinkedHashMap이 단계 정렬을 보존한다.
        // 빈 단계는 currency=null·count=0인 단일 행으로 들어와 count 0·amounts 빈 리스트가 된다.
        Map<Long, List<PipelineDistributionRow>> byStage = new LinkedHashMap<>();
        for (PipelineDistributionRow row : pipelineStageRepository.pipelineDistribution(s.scoped(), s.ownerIds())) {
            byStage.computeIfAbsent(row.getStageId(), id -> new ArrayList<>()).add(row);
        }

        List<PipelineDistributionResponse> stages = new ArrayList<>(byStage.size());
        for (List<PipelineDistributionRow> rows : byStage.values()) {
            PipelineDistributionRow first = rows.get(0);
            long count = rows.stream().mapToLong(PipelineDistributionRow::getCount).sum();
            List<CurrencyAmount> amounts = rows.stream()
                    .filter(r -> r.getCurrency() != null)
                    .map(r -> new CurrencyAmount(r.getCurrency(), r.getTotalAmount()))
                    .collect(Collectors.toList());
            stages.add(new PipelineDistributionResponse(
                    first.getStageId(),
                    first.getStageName(),
                    first.getStageOrder(),
                    count,
                    amounts,
                    stageBaseTotal(rows)));
        }
        return new PipelineAnalyticsResponse(currencyConversionPort.baseCurrencyCode(), stages);
    }

    // 단계의 기준통화 합계 — 통화 행들의 base_amount 합. 산정된(not-null) 행이 하나도 없으면 null(0과 미산정 구분).
    private BigDecimal stageBaseTotal(List<PipelineDistributionRow> rows) {
        return rows.stream()
                .map(PipelineDistributionRow::getBaseTotal)
                .filter(b -> b != null)
                .reduce(BigDecimal::add)
                .orElse(null);
    }

    public List<LeadStatusCountResponse> getLeadsByStatus() {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();
        Map<LeadStatus, Long> countMap = leadRepository.countByStatusGrouped(s.scoped(), s.ownerIds()).stream()
                .collect(Collectors.toMap(LeadStatusCountRow::getStatus, LeadStatusCountRow::getCount));

        return Arrays.stream(LeadStatus.values())
                .map(status -> new LeadStatusCountResponse(status, countMap.getOrDefault(status, 0L)))
                .collect(Collectors.toList());
    }
}
