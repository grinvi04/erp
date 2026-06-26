package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
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
    private final PermissionChecker permissionChecker;
    private final CrmDataScopeResolver dataScopeResolver;

    public PageResponse<LeadResponse> search(LeadStatus status, String keyword, Pageable pageable) {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();
        return PageResponse.from(
                leadRepository.search(status, keyword, s.scoped(), s.ownerIds(), pageable)
                .map(LeadResponse::from));
    }

    public LeadResponse findById(Long id) {
        permissionChecker.require(Permission.CRM_READ);
        var lead = getOrThrow(id);
        dataScopeResolver.requireOwnerAccess(lead.getOwnerId());
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse create(LeadCreateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        Lead lead = Lead.of(req.lastName(), req.firstName(), req.company(), req.title(),
                req.email(), req.phone(), req.source(), req.ownerId(), req.note());
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse update(Long id, LeadUpdateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        Lead lead = getOrThrow(id);
        lead.checkVersion(req.version());
        if (lead.isConverted()) {
            throw new ErpException(ErrorCode.LEAD_ALREADY_CONVERTED_UPDATE);
        }
        lead.update(req.lastName(), req.firstName(), req.company(), req.title(),
                req.email(), req.phone(), req.source(), req.ownerId(), req.note());
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse convert(Long id, LeadConvertRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
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
        permissionChecker.require(Permission.CRM_WRITE);
        getOrThrow(id).softDelete();
    }

    private Lead getOrThrow(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.LEAD_NOT_FOUND));
    }
}
