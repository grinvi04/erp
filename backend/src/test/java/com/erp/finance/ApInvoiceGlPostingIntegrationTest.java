package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoiceLineRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AP 전표 승인 → GL 자동 분개(DRAFT) 전체 흐름 검증.
 * 실무 분개: (차) 비용[라인 계정] / (대) 외상매입금[공급업체 통제계정] — 균형·DRAFT·역참조 확인.
 */
@Transactional
class ApInvoiceGlPostingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ApInvoiceService apInvoiceService;
    @Autowired private ApInvoiceRepository invoiceRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    private Long expenseAccountId;
    private Long vendorId;

    @BeforeEach
    void setUp() {
        FiscalYear fy = fiscalYearRepository.save(FiscalYear.of(2025,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        fiscalPeriodRepository.save(FiscalPeriod.of(fy, 1,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        Account expense = accountRepository.save(Account.of("51100", "소모품비",
                AccountType.EXPENSE, NormalBalance.DEBIT, null, false));
        Account apControl = accountRepository.save(Account.of("25100", "외상매입금",
                AccountType.LIABILITY, NormalBalance.CREDIT, null, false));
        expenseAccountId = expense.getId();

        Vendor vendor = Vendor.of("V-GL", "공급사", "111-11-11111", "담당", "v@test.com", "010-1", 30);
        vendor.assignPayablesAccount(apControl);
        vendorId = vendorRepository.save(vendor).getId();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub, BigDecimal approvalLimit, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        List<GrantedAuthority> auths = java.util.Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, auths));
        accessProfileRepository.findByTenantIdAndUserId(TEST_TENANT_ID, sub)
                .map(p -> { p.update(DataScope.ALL, null, approvalLimit); return p; })
                .orElseGet(() -> accessProfileRepository.save(
                        UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, approvalLimit)));
    }

    @Test
    void approve_withLinesAndVendorPayables_createsBalancedDraftJournalEntry() {
        // 작성자: 라인(소모품비 10만)이 있는 전표 생성·상신
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = apInvoiceService.create(new ApInvoiceCreateRequest(
                "INV-GL-1", vendorId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("100000"), "KRW", null,
                List.of(new ApInvoiceLineRequest(expenseAccountId, new BigDecimal("100000"), "소모품 구매"))));
        apInvoiceService.submit(created.id());

        // 결재자: 승인 → GL 자동 분개
        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        var approved = apInvoiceService.approve(created.id());

        assertThat(approved.journalEntryId()).isNotNull();
        JournalEntry je = journalEntryRepository.findById(approved.journalEntryId()).orElseThrow();
        assertThat(je.getStatus().name()).isEqualTo("DRAFT");
        assertThat(je.isBalanced()).isTrue();
        assertThat(je.getTotalDebit()).isEqualByComparingTo("100000");
        assertThat(je.getTotalCredit()).isEqualByComparingTo("100000");
        assertThat(je.getReferenceType()).isEqualTo("AP_INVOICE");
        assertThat(je.getReferenceId()).isEqualTo(created.id());
    }

    @Test
    void approve_withoutLines_doesNotPost() {
        // 라인 없는 전표(레거시) → 자동 분개 생략(추가적 변화 없음, fail-closed 아님)
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = apInvoiceService.create(new ApInvoiceCreateRequest(
                "INV-GL-2", vendorId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("50000"), "KRW", null, null));
        apInvoiceService.submit(created.id());

        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        var approved = apInvoiceService.approve(created.id());

        assertThat(approved.journalEntryId()).isNull();
        assertThat(invoiceRepository.findById(created.id()).orElseThrow().getStatus().name())
                .isEqualTo("APPROVED");
    }

    @Test
    void pay_withCashAccount_createsBalancedPaymentDraftJournalEntry() {
        // 승인된 전표 준비
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = apInvoiceService.create(new ApInvoiceCreateRequest(
                "INV-PAY-1", vendorId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("100000"), "KRW", null,
                List.of(new ApInvoiceLineRequest(expenseAccountId, new BigDecimal("100000"), "구매"))));
        apInvoiceService.submit(created.id());
        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        apInvoiceService.approve(created.id());

        // 현금 계정으로 지급 → (차)외상매입금 /(대)현금 분개
        Account cash = accountRepository.save(Account.of("10100", "현금",
                AccountType.ASSET, NormalBalance.DEBIT, null, false));
        authenticate("payer", new BigDecimal("1000000"), "finance:invoice:pay");
        apInvoiceService.pay(created.id(), new ApInvoicePayRequest(
                new BigDecimal("100000"), cash.getId(), LocalDate.of(2025, 1, 20)));

        JournalEntry payJe = journalEntryRepository
                .findByReferenceTypeAndReferenceId("AP_PAYMENT", created.id()).orElseThrow();
        assertThat(payJe.getStatus().name()).isEqualTo("DRAFT");
        assertThat(payJe.isBalanced()).isTrue();
        assertThat(payJe.getTotalDebit()).isEqualByComparingTo("100000");
        // 대변이 현금 계정(지급으로 현금 감소)
        assertThat(payJe.getLines()).anyMatch(l -> l.getCreditAmount().compareTo(BigDecimal.ZERO) > 0
                && l.getAccount().getId().equals(cash.getId()));
    }
}
