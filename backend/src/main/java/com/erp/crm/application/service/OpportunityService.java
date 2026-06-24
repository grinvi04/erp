package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.application.dto.OpportunityUpdateRequest;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CrmAccountService accountService;
    private final PipelineStageService stageService;

    public PageResponse<OpportunityResponse> search(Long accountId, Long stageId, Pageable pageable) {
        return PageResponse.from(opportunityRepository.search(accountId, stageId, pageable)
                .map(OpportunityResponse::from));
    }

    public OpportunityResponse findById(Long id) {
        return OpportunityResponse.from(getOrThrow(id));
    }

    @Transactional
    public OpportunityResponse create(OpportunityCreateRequest req) {
        Opportunity opportunity = Opportunity.of(
                accountService.getOrThrow(req.accountId()),
                req.name(),
                stageService.getOrThrow(req.stageId()),
                req.amount(), req.currency(), req.closeDate(), req.probability(),
                req.ownerId(), req.source(), req.description());
        return OpportunityResponse.from(opportunityRepository.save(opportunity));
    }

    @Transactional
    public OpportunityResponse update(Long id, OpportunityUpdateRequest req) {
        Opportunity opportunity = getOrThrow(id);
        opportunity.update(req.name(), stageService.getOrThrow(req.stageId()),
                req.amount(), req.currency(), req.closeDate(), req.probability(),
                req.ownerId(), req.source(), req.description());
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id).softDelete();
    }

    public Opportunity getOrThrow(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.OPPORTUNITY_NOT_FOUND));
    }
}
