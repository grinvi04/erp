package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.Permission;
import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.application.service.SalesTeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 영업팀 관리 — 팀 CRUD와 멤버 추가/제거가 DB 정본을 거쳐 동작하는지 검증한다(권한·테넌트 필터 포함).
 */
@Transactional
class SalesTeamIntegrationTest extends AbstractIntegrationTest {

    @Autowired private SalesTeamService salesTeamService;

    @BeforeEach
    void authAsAdmin() {
        authenticate("admin", Permission.IAM_READ, Permission.IAM_WRITE);
    }

    @Test
    void create_update_delete_roundTrip() {
        SalesTeamResponse created = salesTeamService.createTeam("ST-01", "강남팀");
        assertThat(created.id()).isNotNull();
        assertThat(created.version()).isZero();

        SalesTeamResponse updated = salesTeamService.updateTeam(
                created.id(), "강남2팀", created.version());
        assertThat(updated.name()).isEqualTo("강남2팀");

        assertThat(salesTeamService.listTeams()).extracting(SalesTeamResponse::code)
                .contains("ST-01");

        salesTeamService.deleteTeam(created.id());
        assertThat(salesTeamService.listTeams()).extracting(SalesTeamResponse::code)
                .doesNotContain("ST-01");
    }

    @Test
    void addMember_removeMember_persisted() {
        SalesTeamResponse team = salesTeamService.createTeam("ST-02", "판교팀");

        salesTeamService.addMember(team.id(), "user-001");
        salesTeamService.addMember(team.id(), "user-002");
        salesTeamService.addMember(team.id(), "user-001"); // 멱등

        assertThat(salesTeamService.getTeam(team.id()).memberUserIds())
                .containsExactlyInAnyOrder("user-001", "user-002");

        salesTeamService.removeMember(team.id(), "user-001");

        assertThat(salesTeamService.getTeam(team.id()).memberUserIds())
                .containsExactly("user-002");
    }
}
