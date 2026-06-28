package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.AccountCreateRequest;
import com.erp.crm.application.dto.ContactCreateRequest;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.dto.LeadUpdateRequest;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.model.PipelineStage;
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
  private final ContactService contactService;
  private final OpportunityService opportunityService;
  private final PipelineStageService stageService;
  private final PermissionChecker permissionChecker;
  private final CrmDataScopeResolver dataScopeResolver;
  private final CurrentUserProvider currentUserProvider;

  public PageResponse<LeadResponse> search(LeadStatus status, String keyword, Pageable pageable) {
    permissionChecker.require(Permission.CRM_READ);
    var s = dataScopeResolver.ownerScope();
    return PageResponse.from(
        leadRepository
            .search(status, keyword, s.scoped(), s.ownerIds(), pageable)
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
    Lead lead =
        Lead.of(
            req.lastName(),
            req.firstName(),
            req.company(),
            req.title(),
            req.email(),
            req.phone(),
            req.source(),
            currentUserProvider.getCurrentUserId(),
            req.note());
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
    lead.update(
        req.lastName(),
        req.firstName(),
        req.company(),
        req.title(),
        req.email(),
        req.phone(),
        req.source(),
        req.ownerId(),
        req.note());
    leadRepository.flush();
    return LeadResponse.from(lead);
  }

  /**
   * 리드 전환 — 상용 CRM 표준. 하나의 트랜잭션 안에서 고객사·담당자(·영업기회)를 생성하고 리드 데이터를 이관한 뒤 리드를 CONVERTED로 마킹한다.
   *
   * <ul>
   *   <li>고객사: accountId가 있으면 기존 고객사, 없으면 리드의 회사·전화로 신규 생성(코드 {@code LEAD-{id}}).
   *   <li>담당자: 항상 리드의 이름·이메일·전화로 생성하고 위 고객사에 연결(신규 고객사면 주 담당자).
   *   <li>영업기회: createOpportunity가 true일 때만 생성(단계 확률 상속, 리드 출처 이관).
   * </ul>
   */
  @Transactional
  public LeadResponse convert(Long id, LeadConvertRequest req) {
    permissionChecker.require(Permission.CRM_WRITE);
    Lead lead = getOrThrow(id);
    if (lead.isConverted()) {
      throw new ErpException(ErrorCode.LEAD_ALREADY_CONVERTED);
    }

    boolean newAccount = req.accountId() == null;
    Long accountId =
        newAccount ? accountService.create(buildAccountRequest(lead)).id() : req.accountId();

    Long contactId = contactService.create(buildContactRequest(lead, accountId, newAccount)).id();

    Long opportunityId = null;
    if (req.createOpportunity()) {
      if (req.stageId() == null) {
        throw new ErpException(ErrorCode.LEAD_CONVERT_STAGE_REQUIRED);
      }
      opportunityId = opportunityService.create(buildOpportunityRequest(lead, accountId, req)).id();
    }

    Account account = accountService.getOrThrow(accountId);
    lead.convert(account, contactId, opportunityId);
    return LeadResponse.from(lead);
  }

  private AccountCreateRequest buildAccountRequest(Lead lead) {
    String name =
        lead.getCompany() != null && !lead.getCompany().isBlank()
            ? lead.getCompany()
            : lead.getLastName() + lead.getFirstName();
    return new AccountCreateRequest(
        "LEAD-" + lead.getId(),
        name,
        null,
        null,
        null,
        lead.getPhone(),
        null,
        null,
        null,
        AccountType.CUSTOMER);
  }

  private ContactCreateRequest buildContactRequest(Lead lead, Long accountId, boolean isPrimary) {
    return new ContactCreateRequest(
        accountId,
        lead.getLastName(),
        lead.getFirstName(),
        lead.getTitle(),
        null,
        lead.getEmail(),
        lead.getPhone(),
        null,
        isPrimary);
  }

  private OpportunityCreateRequest buildOpportunityRequest(
      Lead lead, Long accountId, LeadConvertRequest req) {
    PipelineStage stage = stageService.getOrThrow(req.stageId());
    String name =
        req.opportunityName() != null && !req.opportunityName().isBlank()
            ? req.opportunityName()
            : (lead.getCompany() != null && !lead.getCompany().isBlank()
                ? lead.getCompany()
                : lead.getLastName() + lead.getFirstName());
    return new OpportunityCreateRequest(
        accountId,
        name,
        req.stageId(),
        req.opportunityAmount(),
        req.opportunityCurrency(),
        req.opportunityCloseDate(),
        stage.getProbability(),
        lead.getSource(),
        null);
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.CRM_WRITE);
    getOrThrow(id).softDelete();
  }

  private Lead getOrThrow(Long id) {
    return leadRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.LEAD_NOT_FOUND));
  }
}
