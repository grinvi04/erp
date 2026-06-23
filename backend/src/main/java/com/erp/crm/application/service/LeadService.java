package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.dto.LeadUpdateRequest;
import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeadService {

    private final LeadRepository leadRepository;
    private final CrmAccountService accountService;
    private final OpportunityService opportunityService;

    public PageResponse<LeadResponse> search(LeadStatus status, String keyword, Pageable pageable) {
        return PageResponse.from(leadRepository.search(status, keyword, pageable)
                .map(LeadResponse::from));
    }

    public LeadResponse findById(Long id) {
        return LeadResponse.from(getOrThrow(id));
    }

    @Transactional
    public LeadResponse create(LeadCreateRequest req) {
        Lead lead = Lead.of(req.lastName(), req.firstName(), req.company(), req.title(),
                req.email(), req.phone(), req.source(), req.ownerId(), req.note());
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse update(Long id, LeadUpdateRequest req) {
        Lead lead = getOrThrow(id);
        if (lead.isConverted()) {
            throw new ErpException(ErrorCode.LEAD_ALREADY_CONVERTED_UPDATE);
        }
        lead.update(req.lastName(), req.firstName(), req.company(), req.title(),
                req.email(), req.phone(), req.source(), req.ownerId(), req.note());
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse convert(Long id, LeadConvertRequest req) {
        Lead lead = getOrThrow(id);
        if (lead.isConverted()) {
            throw new ErpException(ErrorCode.LEAD_ALREADY_CONVERTED);
        }
        if (req.opportunityId() != null) {
            opportunityService.getOrThrow(req.opportunityId());
        }
        lead.convert(accountService.getOrThrow(req.accountId()), req.opportunityId());
        return LeadResponse.from(lead);
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id).softDelete();
    }

    private Lead getOrThrow(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.LEAD_NOT_FOUND));
    }
}
