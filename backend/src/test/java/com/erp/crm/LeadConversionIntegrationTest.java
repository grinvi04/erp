package com.erp.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.Permission;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.service.LeadService;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Contact;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.ContactRepository;
import com.erp.crm.domain.repository.CrmAccountRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리드 전환 실동작 검증(T1-9) — 전환 시 고객사·담당자(·영업기회)가 실제로 생성되고 리드 데이터가 이관·연결되는지 확인한다. 빈 껍데기(상태만 변경)가 아님을
 * 보장한다.
 */
@Transactional
class LeadConversionIntegrationTest extends AbstractIntegrationTest {

  private static final String SALES = "sales-001";

  @Autowired private LeadService leadService;
  @Autowired private CrmAccountRepository accountRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private OpportunityRepository opportunityRepository;
  @Autowired private PipelineStageRepository pipelineStageRepository;

  @BeforeEach
  void auth() {
    authenticate(SALES, Permission.CRM_READ, Permission.CRM_WRITE);
  }

  private LeadResponse createLead() {
    return leadService.create(
        new LeadCreateRequest(
            "김", "철수", "ABC주식회사", "팀장", "kim@abc.com", "010-1234-5678", "WEB", "관심 고객"));
  }

  @Test
  void convert_newAccount_createsAccountAndContactAndTransfersLeadData() {
    LeadResponse lead = createLead();

    LeadResponse result =
        leadService.convert(
            lead.id(), new LeadConvertRequest(null, false, null, null, null, null, null));

    // 리드: 전환 상태 + 생성 엔티티 연결
    assertThat(result.status()).isEqualTo(LeadStatus.CONVERTED);
    assertThat(result.convertedAccountId()).isNotNull();
    assertThat(result.convertedContactId()).isNotNull();
    assertThat(result.convertedOpportunityId()).isNull();

    // 고객사: 리드의 회사·전화 이관
    Account account = accountRepository.findById(result.convertedAccountId()).orElseThrow();
    assertThat(account.getName()).isEqualTo("ABC주식회사");
    assertThat(account.getPhone()).isEqualTo("010-1234-5678");
    assertThat(account.getCode()).isEqualTo("LEAD-" + lead.id());

    // 담당자: 리드의 이름·이메일·전화 이관 + 고객사 연결, 신규 고객사이므로 주 담당자
    Contact contact = contactRepository.findById(result.convertedContactId()).orElseThrow();
    assertThat(contact.getLastName()).isEqualTo("김");
    assertThat(contact.getFirstName()).isEqualTo("철수");
    assertThat(contact.getEmail()).isEqualTo("kim@abc.com");
    assertThat(contact.getPhone()).isEqualTo("010-1234-5678");
    assertThat(contact.getAccount().getId()).isEqualTo(account.getId());
    assertThat(contact.isPrimary()).isTrue();
  }

  @Test
  void convert_existingAccount_reusesAccountAndDoesNotCreateNew() {
    LeadResponse lead = createLead();
    Account existing =
        accountRepository.save(
            Account.of(
                "ACC-EXIST",
                "기존고객사",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountType.CUSTOMER,
                SALES));
    long before = accountRepository.count();

    LeadResponse result =
        leadService.convert(
            lead.id(),
            new LeadConvertRequest(existing.getId(), false, null, null, null, null, null));

    assertThat(result.convertedAccountId()).isEqualTo(existing.getId());
    assertThat(accountRepository.count()).as("기존 고객사 재사용 — 신규 생성 없음").isEqualTo(before);
    Contact contact = contactRepository.findById(result.convertedContactId()).orElseThrow();
    assertThat(contact.getAccount().getId()).isEqualTo(existing.getId());
    assertThat(contact.isPrimary()).as("기존 고객사 전환 시 주 담당자로 강제하지 않음").isFalse();
  }

  @Test
  void convert_withCreateOpportunity_createsOpportunityFromLead() {
    LeadResponse lead = createLead();
    PipelineStage stage = pipelineStageRepository.save(PipelineStage.of("협상", 3, 60, false, false));

    LeadResponse result =
        leadService.convert(
            lead.id(),
            new LeadConvertRequest(
                null,
                true,
                "ABC 도입 건",
                stage.getId(),
                BigDecimal.valueOf(5_000_000),
                "KRW",
                LocalDate.of(2026, 12, 31)));

    assertThat(result.convertedOpportunityId()).isNotNull();
    Opportunity opp = opportunityRepository.findById(result.convertedOpportunityId()).orElseThrow();
    assertThat(opp.getName()).isEqualTo("ABC 도입 건");
    assertThat(opp.getStage().getId()).isEqualTo(stage.getId());
    assertThat(opp.getProbability()).as("단계 확률 상속").isEqualTo(60);
    assertThat(opp.getSource()).as("리드 출처 이관").isEqualTo("WEB");
    assertThat(opp.getAccount().getId()).isEqualTo(result.convertedAccountId());
    assertThat(opp.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
  }
}
