package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.DataScope;
import com.erp.common.security.Permission;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.service.LeadService;
import com.erp.crm.application.service.SalesTeamService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CRM 데이터 스코프 격리 — 현재 사용자의 DataScope(SELF/DEPARTMENT/ALL)에 따라 Lead 조회가
 * owner 기준으로 좁혀지는지 검증한다. (Opportunity·Account·Activity도 동일 CrmDataScopeResolver·
 * 쿼리 패턴을 공유하므로 Lead로 계약을 고정한다.)
 */
@Transactional
class CrmDataScopeIntegrationTest extends AbstractIntegrationTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CAROL = "carol";

    @Autowired private LeadService leadService;
    @Autowired private SalesTeamService salesTeamService;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    @BeforeEach
    void authAsAlice() {
        authenticate(ALICE, Permission.CRM_READ, Permission.CRM_WRITE, Permission.IAM_WRITE);
    }

    @Test
    void self_scope_seesOnlyOwnLeads() {
        createLeadOwnedBy(ALICE);
        createLeadOwnedBy(BOB);
        setScope(DataScope.SELF);

        List<LeadResponse> visible = search();

        assertThat(visible).extracting(LeadResponse::ownerId).containsExactly(ALICE);
    }

    @Test
    void all_scope_seesEveryLead() {
        createLeadOwnedBy(ALICE);
        createLeadOwnedBy(BOB);
        createLeadOwnedBy(CAROL);
        setScope(DataScope.ALL);

        List<LeadResponse> visible = search();

        assertThat(visible).extracting(LeadResponse::ownerId)
                .containsExactlyInAnyOrder(ALICE, BOB, CAROL);
    }

    @Test
    void department_scope_seesTeammatesButNotOutsiders() {
        // alice·bob 같은 팀, carol 외부.
        var team = salesTeamService.createTeam("ST-DS", "스코프팀");
        salesTeamService.addMember(team.id(), ALICE);
        salesTeamService.addMember(team.id(), BOB);
        createLeadOwnedBy(ALICE);
        createLeadOwnedBy(BOB);
        createLeadOwnedBy(CAROL);
        setScope(DataScope.DEPARTMENT);

        List<LeadResponse> visible = search();

        assertThat(visible).extracting(LeadResponse::ownerId)
                .containsExactlyInAnyOrder(ALICE, BOB);
    }

    @Test
    void department_scope_noTeam_seesOnlySelf() {
        createLeadOwnedBy(ALICE);
        createLeadOwnedBy(BOB);
        setScope(DataScope.DEPARTMENT);

        List<LeadResponse> visible = search();

        assertThat(visible).extracting(LeadResponse::ownerId).containsExactly(ALICE);
    }

    @Test
    void self_scope_findById_otherOwner_isNotFound() {
        // 목록만 좁히면 id 순회로 우회 가능 — 상세조회도 스코프 밖이면 RESOURCE_NOT_FOUND.
        LeadResponse bobLead = asUser(BOB, () -> leadService.create(new LeadCreateRequest(
                "Lead", BOB, null, null, null, null, null, null)));
        setScope(DataScope.SELF);

        assertThatThrownBy(() -> leadService.findById(bobLead.id()))
                .isInstanceOf(ErpException.class)
                .extracting(e -> ((ErpException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void create_assignsOwnerToAuthenticatedUser() {
        // 생성 시 owner는 요청값이 아니라 현재 인증 사용자(JWT subject)로 자동배정된다.
        LeadResponse created = leadService.create(new LeadCreateRequest(
                "Lead", "any", null, null, null, null, null, null));

        assertThat(created.ownerId()).isEqualTo(ALICE);
    }

    private List<LeadResponse> search() {
        return leadService.search(null, null, PageRequest.of(0, 50)).content();
    }

    private void createLeadOwnedBy(String ownerId) {
        asUser(ownerId, () -> leadService.create(new LeadCreateRequest(
                "Lead", ownerId, null, null, null, null, null, null)));
    }

    /** create는 owner를 현재 인증 사용자로 자동배정하므로, 특정 owner의 리드를 만들려면 그 사용자로 인증한 채 실행한다. */
    private <T> T asUser(String subject, java.util.function.Supplier<T> action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        authenticate(subject, Permission.CRM_WRITE);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private void setScope(DataScope scope) {
        accessProfileRepository.save(UserAccessProfile.of(TEST_TENANT_ID, ALICE, scope, null, null));
    }
}
