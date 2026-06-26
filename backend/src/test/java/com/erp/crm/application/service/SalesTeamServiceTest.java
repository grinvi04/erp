package com.erp.crm.application.service;

import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.domain.model.SalesTeam;
import com.erp.crm.domain.repository.SalesTeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SalesTeamServiceTest {

    @Mock private SalesTeamRepository salesTeamRepository;
    @Mock private PermissionChecker permissionChecker;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private SalesTeamService salesTeamService;

    @Test
    void createTeam_newCode_savesAndReturns() {
        given(salesTeamRepository.existsByCode("ST-01")).willReturn(false);
        given(salesTeamRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        SalesTeamResponse result = salesTeamService.createTeam("ST-01", "강남팀");

        assertThat(result.code()).isEqualTo("ST-01");
        assertThat(result.name()).isEqualTo("강남팀");
    }

    @Test
    void createTeam_duplicateCode_throwsDuplicateCode() {
        given(salesTeamRepository.existsByCode("ST-01")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class,
                () -> salesTeamService.createTeam("ST-01", "강남팀"));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void getTeam_notFound_throwsResourceNotFound() {
        given(salesTeamRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> salesTeamService.getTeam(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateTeam_versionMismatch_throwsOptimisticLock() {
        SalesTeam team = SalesTeam.of("ST-01", "강남팀"); // 미영속 — version=null
        given(salesTeamRepository.findById(1L)).willReturn(Optional.of(team));

        // 기대 version(1)과 실제(null) 불일치 → lost update 방지
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> salesTeamService.updateTeam(1L, "강남2팀", 1L));
    }

    @Test
    void addMember_idempotent_noDuplicateUser() {
        SalesTeam team = SalesTeam.of("ST-01", "강남팀");
        given(salesTeamRepository.findById(1L)).willReturn(Optional.of(team));

        salesTeamService.addMember(1L, "user-001");
        SalesTeamResponse result = salesTeamService.addMember(1L, "user-001");

        assertThat(result.memberUserIds()).containsExactly("user-001");
    }

    @Test
    void removeMember_removesUser() {
        SalesTeam team = SalesTeam.of("ST-01", "강남팀");
        team.addMember("user-001");
        team.addMember("user-002");
        given(salesTeamRepository.findById(1L)).willReturn(Optional.of(team));

        SalesTeamResponse result = salesTeamService.removeMember(1L, "user-001");

        assertThat(result.memberUserIds()).containsExactly("user-002");
    }
}
