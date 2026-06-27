package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.common.workflow.ApprovalInboxService;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * AP 전표 전결규정(위임전결) 결재 권한 통합 검증 — 전결권(권한)·전결 한도(금액)·직무분리를 실제 SecurityContext(권한 authority +
 * approval_limit 클레임) 기준으로 확인한다. 권한·클레임 검사는 Mockito 단위테스트로는 검증되지 않으므로(프록시·SecurityContext 부재) 전체
 * 컨텍스트 통합테스트로 보강한다.
 */
@Transactional
class ApInvoiceApprovalAuthorityIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ApInvoiceService apInvoiceService;
  @Autowired private ApInvoiceRepository invoiceRepository;
  @Autowired private VendorRepository vendorRepository;
  @Autowired private ApprovalInboxService approvalInboxService;
  @Autowired private UserAccessProfileRepository accessProfileRepository;

  private static final String CREATOR = "creator-user";
  private static final String APPROVER = "approver-user";

  private Long invoiceId;

  @BeforeEach
  void setUp() {
    // 작성자(CREATOR)로 전표 생성·상신 → createdBy=CREATOR, 상태 PENDING_APPROVAL, 금액 100만.
    authenticateWithLimit(CREATOR, "0", "finance:write");
    Vendor vendor =
        vendorRepository.save(
            Vendor.of("V-AP", "공급사", "111-11-11111", "담당", "v@test.com", "010-1111-2222", 30));
    ApInvoice inv =
        invoiceRepository.save(
            ApInvoice.create(
                "INV-AP-1",
                vendor,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 1),
                new BigDecimal("1000000"),
                "KRW",
                null));
    apInvoiceService.submit(inv.getId());
    invoiceId = inv.getId();
  }

  private void authenticateWithLimit(String sub, String approvalLimit, String... authorities) {
    // 신원(sub·tenant_id)은 JWT, 전결 한도는 DB 접근 프로파일에서 해석(전면 DB 전환).
    authenticate(sub, authorities);
    // 동일 사용자 재인증(setUp의 CREATOR 등) 시 유니크 제약 위반을 피하려 upsert.
    BigDecimal limit = new BigDecimal(approvalLimit);
    UserAccessProfile profile =
        accessProfileRepository
            .findByTenantIdAndUserId(TEST_TENANT_ID, sub)
            .map(
                existing -> {
                  existing.update(DataScope.ALL, null, limit);
                  return existing;
                })
            .orElseGet(() -> UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, limit));
    accessProfileRepository.save(profile);
  }

  @Test
  void approve_authorizedApproverWithinLimit_approves() {
    // 전결권 보유 + 전결 한도(200만) ≥ 금액(100만) + 작성자 아님 → 승인.
    authenticateWithLimit(APPROVER, "2000000", "finance:invoice:approve");

    ApInvoiceResponse result = apInvoiceService.approve(invoiceId);

    assertThat(result.status().name()).isEqualTo("APPROVED");
  }

  @Test
  void approve_amountExceedsApproverLimit_throwsLimitExceeded() {
    // 전결 한도(50만) < 금액(100만) → 전결규정상 상위 전결권자 필요.
    authenticateWithLimit(APPROVER, "500000", "finance:invoice:approve");

    ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
    assertThat(invoiceRepository.findById(invoiceId).orElseThrow().getStatus().name())
        .isEqualTo("PENDING_APPROVAL");
  }

  @Test
  void approve_withoutApprovePermission_throwsForbidden() {
    // 전결권(finance:invoice:approve) 미보유 — 한도가 충분해도 결재 불가.
    authenticateWithLimit(APPROVER, "2000000");

    ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void inbox_authorizedApproverWithinLimit_seesPendingInvoice() {
    // 전결함 라우팅: 전결권+한도 보유 결재자에게 대기 전표가 보인다(person-assigned 아님).
    authenticateWithLimit(APPROVER, "2000000", "finance:invoice:approve");

    List<ApprovalSummaryResponse> inbox = approvalInboxService.pendingForCurrentUser();

    assertThat(inbox)
        .extracting(ApprovalSummaryResponse::entityType, ApprovalSummaryResponse::entityId)
        .contains(tuple(ReferenceTypes.AP_INVOICE, invoiceId));
  }

  @Test
  void inbox_creator_doesNotSeeOwnInvoice() {
    // 직무분리: 작성자에게는 본인 작성 전표가 결재함에 보이지 않는다(과거엔 잘못 노출됐음).
    authenticateWithLimit(CREATOR, "2000000", "finance:invoice:approve");

    List<ApprovalSummaryResponse> inbox = approvalInboxService.pendingForCurrentUser();

    assertThat(inbox).extracting(ApprovalSummaryResponse::entityId).doesNotContain(invoiceId);
  }

  @Test
  void inbox_amountExceedsLimit_doesNotSeeInvoice() {
    // 전결 한도 미달 결재자에게는 보이지 않는다 — 상위 전결권자에게만.
    authenticateWithLimit(APPROVER, "500000", "finance:invoice:approve");

    List<ApprovalSummaryResponse> inbox = approvalInboxService.pendingForCurrentUser();

    assertThat(inbox).extracting(ApprovalSummaryResponse::entityId).doesNotContain(invoiceId);
  }

  @Test
  void approve_byCreator_throwsNotAuthorized() {
    // 직무분리: 작성자는 전결권·한도가 있어도 본인 작성 전표를 결재할 수 없다.
    authenticateWithLimit(CREATOR, "2000000", "finance:invoice:approve");

    ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
  }
}
