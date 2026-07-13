package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.CompanyProfileUpdateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.CompanyProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FinanceSettingOptimisticLockIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private CompanyProfileService companyProfileService;
  @Autowired private EntityManager entityManager;

  @Test
  void baseCurrencyRejectsUpdateWithStaleVersion() {
    authenticate("setting-admin", "finance:setting:write", "finance:read");
    Long version =
        baseCurrencyService
            .updateBaseCurrency(new BaseCurrencyUpdateRequest("KRW", null))
            .version();
    flushAndClear();

    baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD", version));
    flushAndClear();

    assertThatThrownBy(
            () ->
                baseCurrencyService.updateBaseCurrency(
                    new BaseCurrencyUpdateRequest("EUR", version)))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    assertThat(baseCurrencyService.getBaseCurrency().baseCurrency()).isEqualTo("USD");
  }

  @Test
  void companyProfileRejectsUpdateWithStaleVersion() {
    authenticate("setting-admin", "finance:setting:write", "finance:read");
    Long version =
        companyProfileService
            .updateCompanyProfile(
                new CompanyProfileUpdateRequest(
                    "초기 상호", "1208147521", null, null, null, null, null))
            .version();
    flushAndClear();

    companyProfileService.updateCompanyProfile(
        new CompanyProfileUpdateRequest("변경 상호", "1208147521", null, null, null, null, version));
    flushAndClear();

    assertThatThrownBy(
            () ->
                companyProfileService.updateCompanyProfile(
                    new CompanyProfileUpdateRequest(
                        "오래된 변경", "1208147521", null, null, null, null, version)))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    assertThat(companyProfileService.getCompanyProfile().companyName()).isEqualTo("변경 상호");
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
