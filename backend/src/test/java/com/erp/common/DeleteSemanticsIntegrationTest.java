package com.erp.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.application.service.SalesTeamService;
import com.erp.finance.application.dto.BudgetCreateRequest;
import com.erp.finance.application.dto.BudgetResponse;
import com.erp.finance.application.service.BudgetService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.BudgetRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A4 — budget·salesTeam 삭제가 소프트삭제(deleted_at 세팅)인지 검증한다.
 *
 * <p>DB 표준(전 테이블 소프트삭제·감사이력 보존)에 따라 두 엔티티의 삭제는 물리 행 제거가 아니라 deleted_at 세팅이어야 한다. 삭제 후 ①목록/조회에서
 * 제외(@SQLRestriction)되고 ②네이티브 조회로는 물리 행이 deleted_at 세팅된 채 잔존해야 한다.
 */
@Transactional
class DeleteSemanticsIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BudgetService budgetService;
  @Autowired private SalesTeamService salesTeamService;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private BudgetRepository budgetRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void auth() {
    authenticate(
        "admin",
        com.erp.common.security.Permission.FINANCE_READ,
        com.erp.common.security.Permission.FINANCE_WRITE,
        com.erp.common.security.Permission.IAM_READ,
        com.erp.common.security.Permission.IAM_WRITE);
  }

  @Test
  void budgetDelete_isSoftDelete() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    Account account =
        accountRepository.save(
            Account.of("5100", "인건비", AccountType.EXPENSE, NormalBalance.DEBIT, null, false));
    BudgetResponse created =
        budgetService.create(
            new BudgetCreateRequest(fy.getId(), account.getId(), 10L, new BigDecimal("5000000")));

    budgetService.delete(created.id());

    // L1 캐시 회피 — @SQLRestriction이 실제 SELECT에 적용되는지 DB 왕복으로 본다.
    budgetRepository.flush();
    entityManager.clear();

    // 목록/조회에서 제외
    assertThat(budgetService.findByFiscalYear(fy.getId()))
        .extracting(BudgetResponse::id)
        .doesNotContain(created.id());

    // 물리 행은 잔존하고 deleted_at이 세팅됨
    Object deletedAt =
        entityManager
            .createNativeQuery("SELECT deleted_at FROM finance.budget WHERE id = :id")
            .setParameter("id", created.id())
            .getSingleResult();
    assertThat(deletedAt).as("budget 물리 행은 잔존하고 deleted_at이 세팅되어야 한다").isNotNull();
  }

  @Test
  void salesTeamDelete_isSoftDelete() {
    SalesTeamResponse created = salesTeamService.createTeam("ST-DEL", "삭제팀");

    salesTeamService.deleteTeam(created.id());

    entityManager.flush();
    entityManager.clear();

    // 목록에서 제외
    assertThat(salesTeamService.listTeams())
        .extracting(SalesTeamResponse::code)
        .doesNotContain("ST-DEL");

    // 물리 행은 잔존하고 deleted_at이 세팅됨
    Object deletedAt =
        entityManager
            .createNativeQuery("SELECT deleted_at FROM crm.sales_team WHERE id = :id")
            .setParameter("id", created.id())
            .getSingleResult();
    assertThat(deletedAt).as("sales_team 물리 행은 잔존하고 deleted_at이 세팅되어야 한다").isNotNull();
  }
}
