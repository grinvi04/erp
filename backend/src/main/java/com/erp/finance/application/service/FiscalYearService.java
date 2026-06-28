package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.FiscalPeriodCreateRequest;
import com.erp.finance.application.dto.FiscalPeriodResponse;
import com.erp.finance.application.dto.FiscalYearCreateRequest;
import com.erp.finance.application.dto.FiscalYearResponse;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalPeriodStatus;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FiscalYearService {

  private final FiscalYearRepository fiscalYearRepository;
  private final FiscalPeriodRepository fiscalPeriodRepository;
  private final PermissionChecker permissionChecker;

  public List<FiscalYearResponse> findAll() {
    permissionChecker.require(Permission.FINANCE_READ);
    return fiscalYearRepository.findAllByOrderByYearDesc().stream()
        .map(FiscalYearResponse::from)
        .toList();
  }

  public FiscalYearResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return FiscalYearResponse.from(getFiscalYearOrThrow(id));
  }

  public List<FiscalPeriodResponse> findPeriodsByYear(Long fiscalYearId) {
    permissionChecker.require(Permission.FINANCE_READ);
    getFiscalYearOrThrow(fiscalYearId);
    return fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNumberAsc(fiscalYearId).stream()
        .map(FiscalPeriodResponse::from)
        .toList();
  }

  @Transactional
  public FiscalYearResponse create(FiscalYearCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (fiscalYearRepository.existsByYear(request.year())) {
      throw new ErpException(ErrorCode.FISCAL_YEAR_DUPLICATE);
    }
    FiscalYear fy = FiscalYear.of(request.year(), request.startDate(), request.endDate());
    return FiscalYearResponse.from(fiscalYearRepository.save(fy));
  }

  @Transactional
  public FiscalYearResponse close(Long id) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    FiscalYear fy = getFiscalYearOrThrow(id);
    fiscalPeriodRepository
        .findByFiscalYearIdAndStatus(id, FiscalPeriodStatus.OPEN)
        .forEach(FiscalPeriod::close);
    fy.close();
    return FiscalYearResponse.from(fy);
  }

  @Transactional
  public FiscalPeriodResponse createPeriod(Long fiscalYearId, FiscalPeriodCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    FiscalYear fy = getFiscalYearOrThrow(fiscalYearId);
    if (fiscalPeriodRepository.existsByFiscalYearIdAndPeriodNumber(
        fiscalYearId, request.periodNumber())) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_DUPLICATE);
    }
    if (request.startDate().isBefore(fy.getStartDate())
        || request.endDate().isAfter(fy.getEndDate())) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_DATE_OUT_OF_RANGE);
    }
    FiscalPeriod fp =
        FiscalPeriod.of(fy, request.periodNumber(), request.startDate(), request.endDate());
    return FiscalPeriodResponse.from(fiscalPeriodRepository.save(fp));
  }

  @Transactional
  public FiscalPeriodResponse closePeriod(Long periodId) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    FiscalPeriod fp = getFiscalPeriodOrThrow(periodId);
    fp.close();
    return FiscalPeriodResponse.from(fp);
  }

  private FiscalYear getFiscalYearOrThrow(Long id) {
    return fiscalYearRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND));
  }

  private FiscalPeriod getFiscalPeriodOrThrow(Long id) {
    return fiscalPeriodRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));
  }
}
