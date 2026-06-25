package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AP 전표 전결규정(위임전결) 결재 권한 통합 검증 — 전결권(권한)·전결 한도(금액)·직무분리를
 * 실제 SecurityContext(권한 authority + approval_limit 클레임) 기준으로 확인한다.
 * 권한·클레임 검사는 Mockito 단위테스트로는 검증되지 않으므로(프록시·SecurityContext 부재)
 * 전체 컨텍스트 통합테스트로 보강한다.
 */
@Transactional
class ApInvoiceApprovalAuthorityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ApInvoiceService apInvoiceService;
    @Autowired private ApInvoiceRepository invoiceRepository;
    @Autowired private VendorRepository vendorRepository;

    private static final String CREATOR = "creator-user";
    private static final String APPROVER = "approver-user";

    private Long invoiceId;

    @BeforeEach
    void setUp() {
        // 작성자(CREATOR)로 전표 생성·상신 → createdBy=CREATOR, 상태 PENDING_APPROVAL, 금액 100만.
        authenticate(CREATOR, "0", "finance:write");
        Vendor vendor = vendorRepository.save(
            Vendor.of("V-AP", "공급사", "111-11-11111", "담당", "v@test.com", "010-1111-2222", 30));
        ApInvoice inv = invoiceRepository.save(ApInvoice.create("INV-AP-1", vendor,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
            new BigDecimal("1000000"), "KRW", null));
        apInvoiceService.submit(inv.getId());
        invoiceId = inv.getId();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub, String approvalLimit, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
            .subject(sub).claim("sub", sub).claim("approval_limit", approvalLimit).build();
        List<GrantedAuthority> auths = Arrays.stream(authorities)
            .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, auths));
    }

    @Test
    void approve_authorizedApproverWithinLimit_approves() {
        // 전결권 보유 + 전결 한도(200만) ≥ 금액(100만) + 작성자 아님 → 승인.
        authenticate(APPROVER, "2000000", "finance:invoice:approve");

        ApInvoiceResponse result = apInvoiceService.approve(invoiceId);

        assertThat(result.status().name()).isEqualTo("APPROVED");
    }

    @Test
    void approve_amountExceedsApproverLimit_throwsLimitExceeded() {
        // 전결 한도(50만) < 금액(100만) → 전결규정상 상위 전결권자 필요.
        authenticate(APPROVER, "500000", "finance:invoice:approve");

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        assertThat(invoiceRepository.findById(invoiceId).orElseThrow().getStatus().name())
            .isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approve_withoutApprovePermission_throwsForbidden() {
        // 전결권(finance:invoice:approve) 미보유 — 한도가 충분해도 결재 불가.
        authenticate(APPROVER, "2000000");

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void approve_byCreator_throwsNotAuthorized() {
        // 직무분리: 작성자는 전결권·한도가 있어도 본인 작성 전표를 결재할 수 없다.
        authenticate(CREATOR, "2000000", "finance:invoice:approve");

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(invoiceId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }
}
