package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * AC-10: V2007(finance)·V4004(crm) 백필 마이그레이션이 기존 거래에 base_amount를 산정하는지 검증한다. 행 통화 == 테넌트 기준통화면
 * base=원액·rate=1, 비기준 통화는 null(미산정) 유지. 컬럼 추가는 IF NOT EXISTS, UPDATE는 base_amount IS NULL 가드로 멱등하므로
 * 마이그레이션 SQL을 시드 데이터에 재적용해 백필 로직을 직접 검증한다(MovementApproveBackfill 패턴).
 */
@Transactional
class FxTransactionSnapshotBackfillIntegrationTest extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  private String migrationSql(String file) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource("db/migration/" + file).getInputStream(), StandardCharsets.UTF_8);
  }

  @Test
  void backfill_apInvoice_setsBaseForBaseCurrencyRowsAndLeavesForeignNull() throws Exception {
    // 기준통화 KRW 설정
    jdbcTemplate.update(
        "INSERT INTO finance.tenant_base_currency (tenant_id, base_currency) VALUES (?, ?)",
        TEST_TENANT_ID,
        "KRW");
    Long vendorId =
        jdbcTemplate.queryForObject(
            "INSERT INTO finance.vendor (tenant_id, code, name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            TEST_TENANT_ID,
            "V-FX",
            "FX공급사");

    Long krwId = insertApInvoice(vendorId, "AP-KRW", new BigDecimal("100000"), "KRW");
    Long usdId = insertApInvoice(vendorId, "AP-USD", new BigDecimal("100"), "USD");
    // 백필 전: base_amount 미산정
    jdbcTemplate.update(
        "UPDATE finance.ap_invoice SET base_amount = NULL, exchange_rate = NULL WHERE id IN (?, ?)",
        krwId,
        usdId);

    jdbcTemplate.execute(migrationSql("V2007__fx_transaction_snapshot.sql"));

    assertThat(baseAmount("finance.ap_invoice", krwId)).isEqualByComparingTo("100000");
    assertThat(rate("finance.ap_invoice", krwId)).isEqualByComparingTo("1");
    assertThat(baseAmount("finance.ap_invoice", usdId)).isNull();
    assertThat(rate("finance.ap_invoice", usdId)).isNull();
  }

  @Test
  void backfill_opportunity_usesBaseCurrencyJoinAndLeavesForeignNull() throws Exception {
    // crm 백필은 finance.tenant_base_currency를 읽기 조인(크로스스키마 — 마이그레이션 백필 한정)
    jdbcTemplate.update(
        "INSERT INTO finance.tenant_base_currency (tenant_id, base_currency) VALUES (?, ?)",
        TEST_TENANT_ID,
        "KRW");
    Long accountId =
        jdbcTemplate.queryForObject(
            "INSERT INTO crm.account (tenant_id, code, name, owner_id) VALUES (?, ?, ?, ?) RETURNING id",
            Long.class,
            TEST_TENANT_ID,
            "ACC-FX",
            "FX고객사",
            "owner");
    Long stageId =
        jdbcTemplate.queryForObject(
            "INSERT INTO crm.pipeline_stage (tenant_id, name, stage_order) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            TEST_TENANT_ID,
            "탐색",
            1);

    Long krwId = insertOpportunity(accountId, stageId, "OPP-KRW", new BigDecimal("5000000"), "KRW");
    Long usdId = insertOpportunity(accountId, stageId, "OPP-USD", new BigDecimal("1000"), "USD");

    jdbcTemplate.execute(migrationSql("V4004__opportunity_fx_snapshot.sql"));

    assertThat(baseAmount("crm.opportunity", krwId)).isEqualByComparingTo("5000000");
    assertThat(rate("crm.opportunity", krwId)).isEqualByComparingTo("1");
    assertThat(baseAmount("crm.opportunity", usdId)).isNull();
  }

  private Long insertApInvoice(Long vendorId, String no, BigDecimal amount, String currency) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO finance.ap_invoice (tenant_id, invoice_no, vendor_id, invoice_date, due_date, total_amount, currency) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
        Long.class,
        TEST_TENANT_ID,
        no,
        vendorId,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 1, 31),
        amount,
        currency);
  }

  private Long insertOpportunity(
      Long accountId, Long stageId, String name, BigDecimal amount, String currency) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO crm.opportunity (tenant_id, account_id, name, stage_id, amount, currency, owner_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
        Long.class,
        TEST_TENANT_ID,
        accountId,
        name,
        stageId,
        amount,
        currency,
        "owner");
  }

  private BigDecimal baseAmount(String table, Long id) {
    return jdbcTemplate.queryForObject(
        "SELECT base_amount FROM " + table + " WHERE id = ?", BigDecimal.class, id);
  }

  private BigDecimal rate(String table, Long id) {
    return jdbcTemplate.queryForObject(
        "SELECT exchange_rate FROM " + table + " WHERE id = ?", BigDecimal.class, id);
  }
}
