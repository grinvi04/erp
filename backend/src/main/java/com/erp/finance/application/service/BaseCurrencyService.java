package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.domain.model.TenantBaseCurrency;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.TenantBaseCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트 기준통화 설정 — 조회(미설정 시 KRW 기본)·변경(FINANCE_SETTING_WRITE). FiscalYear 마스터 패턴.
 *
 * <p>변경 가드: FX 거래는 생성 시점의 기준통화로 base_amount·exchange_rate를 스냅샷 저장한다(거래시점고정).
 * 이후 기준통화를 바꾸면 과거 스냅샷이 새 통화 코드로 잘못 라벨링돼 집계 의미가 어긋나므로,
 * finance 거래(ap_invoice·ar_invoice·journal_entry)에 base_amount 스냅샷이 하나라도 있으면 변경을 거부한다.
 *
 * <p><b>알려진 한계</b>: CRM Opportunity도 base_amount 스냅샷을 갖지만 모듈 경계상 finance에서 CRM repo를
 * 직접 참조할 수 없어 이 가드의 판정 대상에서 제외한다. 실무상 FX 거래가 생기면 보통 finance 거래(AP/AR/전표)가
 * 함께 존재하므로 가드는 사실상 발동하며, opportunity만 단독으로 스냅샷을 갖는 경우는 한계로 남는다
 * (필요 시 별도 SPI/이벤트로 확장 — 현재는 과한 추상화를 피한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseCurrencyService {

    private final TenantBaseCurrencyRepository repository;
    private final ApInvoiceRepository apInvoiceRepository;
    private final ArInvoiceRepository arInvoiceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PermissionChecker permissionChecker;

    public BaseCurrencyResponse getBaseCurrency() {
        permissionChecker.require(Permission.FINANCE_READ);
        return BaseCurrencyResponse.of(currentBaseCurrencyCode());
    }

    /** 환산 등 내부 사용을 위한 현재 테넌트 기준통화 코드(미설정 시 KRW). 권한 검사 없음. */
    public String currentBaseCurrencyCode() {
        return repository.findFirstByOrderByIdAsc()
            .map(TenantBaseCurrency::getBaseCurrency)
            .orElse(TenantBaseCurrency.DEFAULT_BASE_CURRENCY);
    }

    @Transactional
    public BaseCurrencyResponse updateBaseCurrency(BaseCurrencyUpdateRequest request) {
        permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
        // 실제 통화가 바뀔 때만 가드 — 동일 값 PUT(no-op)·최초 설정(KRW 기본 → KRW)은 허용한다.
        if (!request.baseCurrency().equals(currentBaseCurrencyCode()) && hasBaseAmountSnapshot()) {
            throw new ErpException(ErrorCode.BASE_CURRENCY_CHANGE_NOT_ALLOWED);
        }
        TenantBaseCurrency entity = repository.findFirstByOrderByIdAsc()
            .map(existing -> { existing.changeBaseCurrency(request.baseCurrency()); return existing; })
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(request.baseCurrency())));
        return BaseCurrencyResponse.of(entity.getBaseCurrency());
    }

    /** finance 거래에 base_amount 스냅샷이 하나라도 있으면 true(현재 테넌트만 @TenantId 자동 필터). */
    private boolean hasBaseAmountSnapshot() {
        return apInvoiceRepository.existsByBaseAmountIsNotNull()
            || arInvoiceRepository.existsByBaseAmountIsNotNull()
            || journalEntryRepository.existsByBaseAmountIsNotNull();
    }
}
