package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoiceLineRequest;
import com.erp.finance.application.dto.ArInvoicePayRequest;
import com.erp.finance.application.service.ArInvoiceService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
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
 * AR 전표 승인 → GL 자동 분개(DRAFT) 전체 흐름 검증.
 * 실무 분개(AP와 반전): (차) 외상매출금[고객 통제계정] / (대) 매출[라인 계정] — 균형·DRAFT·역참조 확인.
 * DEBIT 측이 외상매출금 계정임을 검증한다.
 */
@Transactional
class ArInvoiceGlPostingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ArInvoiceService arInvoiceService;
    @Autowired private ArInvoiceRepository invoiceRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    private Long revenueAccountId;
    private Long customerId;
    private Long receivablesAccountId;

    @BeforeEach
    void setUp() {
        FiscalYear fy = fiscalYearRepository.save(FiscalYear.of(2025,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        fiscalPeriodRepository.save(FiscalPeriod.of(fy, 1,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        Account revenue = accountRepository.save(Account.of("40100", "매출",
                AccountType.REVENUE, NormalBalance.CREDIT, null, false));
        Account arControl = accountRepository.save(Account.of("11100", "외상매출금",
                AccountType.ASSET, NormalBalance.DEBIT, null, false));
        revenueAccountId = revenue.getId();
        receivablesAccountId = arControl.getId();

        Customer customer = Customer.of("C-GL", "고객사", "222-22-22222", "담당", "c@test.com", "010-2", 30);
        customer.assignReceivablesAccount(arControl);
        customerId = customerRepository.save(customer).getId();
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
    void approve_withLinesAndCustomerReceivables_createsBalancedDraftJournalEntry() {
        // 작성자: 라인(매출 10만)이 있는 AR 전표 생성·상신
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = arInvoiceService.create(new ArInvoiceCreateRequest(
                "AR-GL-1", customerId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("100000"), "KRW", null,
                List.of(new ArInvoiceLineRequest(revenueAccountId, new BigDecimal("100000"), "매출 발생"))));
        arInvoiceService.submit(created.id());

        // 결재자: 승인 → GL 자동 분개
        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        var approved = arInvoiceService.approve(created.id());

        assertThat(approved.journalEntryId()).isNotNull();
        JournalEntry je = journalEntryRepository.findById(approved.journalEntryId()).orElseThrow();
        assertThat(je.getStatus().name()).isEqualTo("DRAFT");
        assertThat(je.isBalanced()).isTrue();
        // 전표 총액(차변 = 대변 = 10만)
        assertThat(je.getTotalDebit()).isEqualByComparingTo("100000");
        assertThat(je.getTotalCredit()).isEqualByComparingTo("100000");
        // 역참조: AR_INVOICE
        assertThat(je.getReferenceType()).isEqualTo("AR_INVOICE");
        assertThat(je.getReferenceId()).isEqualTo(created.id());
        // 차변 측이 외상매출금 계정임을 확인 (AP와 반전)
        boolean debitIsReceivables = je.getLines().stream()
                .anyMatch(l -> l.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                        && l.getAccount().getId().equals(receivablesAccountId));
        assertThat(debitIsReceivables).isTrue();
    }

    @Test
    void approve_withoutLines_doesNotPost() {
        // 라인 없는 전표(레거시) → 자동 분개 생략
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = arInvoiceService.create(new ArInvoiceCreateRequest(
                "AR-GL-2", customerId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("50000"), "KRW", null, null));
        arInvoiceService.submit(created.id());

        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        var approved = arInvoiceService.approve(created.id());

        assertThat(approved.journalEntryId()).isNull();
        assertThat(invoiceRepository.findById(created.id()).orElseThrow().getStatus().name())
                .isEqualTo("APPROVED");
    }

    @Test
    void pay_withCashAccount_createsBalancedReceiptDraftJournalEntry() {
        // 승인된 AR 전표 준비
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        var created = arInvoiceService.create(new ArInvoiceCreateRequest(
                "AR-PAY-1", customerId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10),
                new BigDecimal("100000"), "KRW", null,
                List.of(new ArInvoiceLineRequest(revenueAccountId, new BigDecimal("100000"), "매출"))));
        arInvoiceService.submit(created.id());
        authenticate("approver", new BigDecimal("1000000"), "finance:invoice:approve");
        arInvoiceService.approve(created.id());

        // 현금 계정으로 수금 → (차)현금 /(대)외상매출금 분개 (AP 지급의 반전)
        Account cash = accountRepository.save(Account.of("10100", "현금",
                AccountType.ASSET, NormalBalance.DEBIT, null, false));
        authenticate("receiver", BigDecimal.ZERO, "finance:invoice:pay");
        arInvoiceService.pay(created.id(), new ArInvoicePayRequest(
                new BigDecimal("100000"), cash.getId(), LocalDate.of(2025, 1, 20)));

        JournalEntry payJe = journalEntryRepository
                .findByReferenceTypeAndReferenceId("AR_PAYMENT", created.id()).orElseThrow();
        assertThat(payJe.getStatus().name()).isEqualTo("DRAFT");
        assertThat(payJe.isBalanced()).isTrue();
        assertThat(payJe.getTotalDebit()).isEqualByComparingTo("100000");
        // 차변이 현금 계정(수금으로 현금 증가)
        assertThat(payJe.getLines()).anyMatch(l -> l.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                && l.getAccount().getId().equals(cash.getId()));
    }
}
