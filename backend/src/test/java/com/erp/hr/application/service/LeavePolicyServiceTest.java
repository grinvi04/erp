package com.erp.hr.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.LeavePolicyCreateRequest;
import com.erp.hr.application.dto.LeavePolicyResponse;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceTest {

  @Mock private LeavePolicyRepository leavePolicyRepository;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;

  @InjectMocks private LeavePolicyService leavePolicyService;

  @Test
  void findAll_returnsAllPolicies() {
    LeavePolicy policy =
        LeavePolicy.of("ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
    given(leavePolicyRepository.findAll()).willReturn(List.of(policy));

    List<LeavePolicyResponse> result = leavePolicyService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).code()).isEqualTo("ANNUAL");
  }

  @Test
  void create_duplicateCode_throwsDuplicateCode() {
    given(leavePolicyRepository.existsByCode("ANNUAL")).willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                leavePolicyService.create(
                    new LeavePolicyCreateRequest(
                        "ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
  }

  @Test
  void create_valid_returnsSavedPolicy() {
    given(leavePolicyRepository.existsByCode("SICK")).willReturn(false);
    LeavePolicy policy = LeavePolicy.of("SICK", "병가", LeavePolicy.LeaveType.SICK, 10, 0, false, 0);
    given(leavePolicyRepository.save(any())).willReturn(policy);

    LeavePolicyResponse result =
        leavePolicyService.create(
            new LeavePolicyCreateRequest(
                "SICK", "병가", LeavePolicy.LeaveType.SICK, 10, 0, false, 0));

    assertThat(result.code()).isEqualTo("SICK");
    assertThat(result.leaveType()).isEqualTo(LeavePolicy.LeaveType.SICK);
  }

  @Test
  void delete_notFound_throwsNotFound() {
    given(leavePolicyRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> leavePolicyService.delete(99L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }
}
