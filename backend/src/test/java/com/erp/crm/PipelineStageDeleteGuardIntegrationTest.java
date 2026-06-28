package com.erp.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.crm.application.service.PipelineStageService;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.CrmAccountRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import com.erp.crm.domain.repository.PipelineStageRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 파이프라인 단계 삭제 가드(T1-8) — 해당 단계를 참조하는 영업 기회가 있으면 삭제를 거부(409)한다. 참조가 없으면 정상 소프트삭제된다. */
@Transactional
class PipelineStageDeleteGuardIntegrationTest extends AbstractIntegrationTest {

  @Autowired private PipelineStageService stageService;
  @Autowired private PipelineStageRepository pipelineStageRepository;
  @Autowired private OpportunityRepository opportunityRepository;
  @Autowired private CrmAccountRepository accountRepository;
  @Autowired private EntityManager entityManager;

  @BeforeEach
  void auth() {
    authenticate("sales-001", Permission.CRM_READ, Permission.CRM_WRITE);
  }

  @Test
  void delete_stageInUseByOpportunity_throwsPipelineStageInUse() {
    PipelineStage stage = pipelineStageRepository.save(PipelineStage.of("협상", 3, 60, false, false));
    Account account =
        accountRepository.save(
            Account.of(
                "ACC-G",
                "가드테스트사",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountType.CUSTOMER,
                "sales-001"));
    opportunityRepository.save(
        Opportunity.of(
            account,
            "진행 기회",
            stage,
            null,
            "KRW",
            LocalDate.of(2026, 12, 31),
            60,
            "sales-001",
            null,
            null));

    assertThatThrownBy(() -> stageService.delete(stage.getId()))
        .isInstanceOf(ErpException.class)
        .extracting(e -> ((ErpException) e).getErrorCode())
        .isEqualTo(ErrorCode.PIPELINE_STAGE_IN_USE);
  }

  @Test
  void delete_unusedStage_softDeletes() {
    PipelineStage stage =
        pipelineStageRepository.save(PipelineStage.of("미사용", 9, 10, false, false));

    stageService.delete(stage.getId());
    entityManager.flush();
    entityManager.clear();

    assertThat(pipelineStageRepository.findById(stage.getId()))
        .as("소프트삭제 — deleted_at IS NULL 필터로 조회 제외")
        .isEmpty();
  }
}
