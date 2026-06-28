package com.erp.crm.application.service;

import com.erp.common.currency.CurrencyConversionPort;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.application.dto.OpportunityUpdateRequest;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.time.LocalDate;
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
  private final PermissionChecker permissionChecker;
  private final CrmDataScopeResolver dataScopeResolver;
  private final CurrentUserProvider currentUserProvider;
  // 모듈 경계: finance를 직접 참조하지 않고 common 환산 포트만 주입(SPI). 구현은 finance CurrencyConverter.
  private final CurrencyConversionPort currencyConversionPort;

  public PageResponse<OpportunityResponse> search(Long accountId, Long stageId, Pageable pageable) {
    permissionChecker.require(Permission.CRM_READ);
    var s = dataScopeResolver.ownerScope();
    return PageResponse.from(
        opportunityRepository
            .search(accountId, stageId, s.scoped(), s.ownerIds(), pageable)
            .map(OpportunityResponse::from));
  }

  public OpportunityResponse findById(Long id) {
    permissionChecker.require(Permission.CRM_READ);
    var opp = getOrThrow(id);
    dataScopeResolver.requireOwnerAccess(opp.getOwnerId());
    return OpportunityResponse.from(opp);
  }

  @Transactional
  public OpportunityResponse create(OpportunityCreateRequest req) {
    permissionChecker.require(Permission.CRM_WRITE);
    Opportunity opportunity =
        Opportunity.of(
            accountService.getOrThrow(req.accountId()),
            req.name(),
            stageService.getOrThrow(req.stageId()),
            req.amount(),
            req.currency(),
            req.closeDate(),
            req.probability(),
            currentUserProvider.getCurrentUserId(),
            req.source(),
            req.description());
    // 거래 시점 FX 스냅샷 — 생성일(오늘) 환율로 amount를 기준통화 환산해 저장.
    // 환율 부재·금액 미정 시 미산정(null) 유지(AC-11).
    currencyConversionPort
        .tryConvert(opportunity.getAmount(), opportunity.getCurrency(), LocalDate.now())
        .ifPresent(c -> opportunity.applyBaseSnapshot(c.baseAmount(), c.rate()));
    return OpportunityResponse.from(opportunityRepository.save(opportunity));
  }

  @Transactional
  public OpportunityResponse update(Long id, OpportunityUpdateRequest req) {
    permissionChecker.require(Permission.CRM_WRITE);
    Opportunity opportunity = getOrThrow(id);
    opportunity.checkVersion(req.version());
    opportunity.update(
        req.name(),
        stageService.getOrThrow(req.stageId()),
        req.amount(),
        req.currency(),
        req.closeDate(),
        req.probability(),
        req.ownerId(),
        req.source(),
        req.description());
    opportunityRepository.flush();
    return OpportunityResponse.from(opportunity);
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.CRM_WRITE);
    getOrThrow(id).softDelete();
  }

  public Opportunity getOrThrow(Long id) {
    return opportunityRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.OPPORTUNITY_NOT_FOUND));
  }
}
