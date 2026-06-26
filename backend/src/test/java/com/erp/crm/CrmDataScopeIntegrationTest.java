package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.Permission;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.service.LeadService;
import com.erp.crm.application.service.SalesTeamService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(ALICE)
                .claim("tenant_id", TEST_TENANT_ID).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(Permission.CRM_READ),
                        new SimpleGrantedAuthority(Permission.CRM_WRITE),
                        new SimpleGrantedAuthority(Permission.IAM_WRITE))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
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

    private List<LeadResponse> search() {
        return leadService.search(null, null, PageRequest.of(0, 50)).content();
    }

    private void createLeadOwnedBy(String ownerId) {
        leadService.create(new LeadCreateRequest(
                "Lead", ownerId, null, null, null, null, null, ownerId, null));
    }

    private void setScope(DataScope scope) {
        accessProfileRepository.save(UserAccessProfile.of(TEST_TENANT_ID, ALICE, scope, null, null));
    }
}
