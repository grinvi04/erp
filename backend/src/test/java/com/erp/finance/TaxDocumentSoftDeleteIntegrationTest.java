package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.CompanyProfile;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.CompanyProfileRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TaxDocumentSoftDeleteIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CompanyProfileRepository companyProfileRepository;
  @Autowired private TaxInvoiceRepository taxInvoiceRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private ArInvoiceRepository arInvoiceRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    authenticate("finance-admin", "finance:read", "finance:write");
  }

  @Test
  void softDeletedCompanyProfile_isExcludedFromCurrentProfileLookup() {
    CompanyProfile profile =
        companyProfileRepository.save(
            CompanyProfile.of("ERP 상사", "1208147521", null, null, null, null));

    profile.softDelete();
    companyProfileRepository.saveAndFlush(profile);
    entityManager.clear();

    assertThat(companyProfileRepository.findFirstByOrderByIdAsc()).isEmpty();
    assertThat(companyProfileRepository.findById(profile.getId())).isEmpty();
  }

  @Test
  void softDeletedTaxInvoice_isExcludedFromStatusList() {
    LocalDate writeDate = LocalDate.of(2025, 6, 30);
    Customer customer =
        customerRepository.save(Customer.of("C-SOFT", "소프트삭제 고객", null, null, null, null, 30));
    ArInvoice arInvoice =
        arInvoiceRepository.save(
            ArInvoice.create(
                "AR-SOFT",
                customer,
                writeDate,
                writeDate.plusDays(30),
                new BigDecimal("100000"),
                TaxType.TAXABLE,
                "KRW",
                null));
    TaxInvoice taxInvoice =
        taxInvoiceRepository.save(
            TaxInvoice.issue(
                arInvoice.getId(),
                TaxType.TAXABLE,
                ChargeType.CHARGE,
                writeDate,
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                new BigDecimal("110000"),
                "품목",
                PartySnapshot.of("ERP 상사", "1208147521", null, null, null, null),
                PartySnapshot.of("소프트삭제 고객", null, null, null, null, null),
                null));

    taxInvoice.softDelete();
    taxInvoiceRepository.saveAndFlush(taxInvoice);
    entityManager.clear();

    assertThat(
            taxInvoiceRepository
                .findByStatus(
                    TaxInvoiceStatus.ISSUED, org.springframework.data.domain.Pageable.unpaged())
                .getContent())
        .isEmpty();
    assertThat(taxInvoiceRepository.findById(taxInvoice.getId())).isEmpty();
  }
}
