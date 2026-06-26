package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.ActivityCreateRequest;
import com.erp.crm.application.dto.ActivityResponse;
import com.erp.crm.domain.model.Activity;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock private com.erp.crm.domain.repository.ActivityRepository activityRepository;
    @Mock private CrmAccountService accountService;
    @Mock private ContactService contactService;
    @Mock private OpportunityService opportunityService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private CrmDataScopeResolver dataScopeResolver;
    @Mock private com.erp.common.security.CurrentUserProvider currentUserProvider;
    @InjectMocks private ActivityService activityService;

    private Activity buildActivity() {
        return Activity.of(ActivityType.CALL, "고객 통화", null, null, null,
                "user-001", null, "1차 컨택");
    }

    @Test
    void create_validRequest_savesAsOpenWithCurrentUserAsOwner() {
        given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
        given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

        ActivityCreateRequest req = new ActivityCreateRequest(ActivityType.CALL, "고객 통화",
                null, null, null, null, "1차 컨택");

        ActivityResponse result = activityService.create(req);

        assertThat(result.subject()).isEqualTo("고객 통화");
        assertThat(result.status()).isEqualTo(ActivityStatus.OPEN);
        assertThat(result.ownerId()).isEqualTo("auth-user-sub");
    }

    @Test
    void findById_notFound_throwsActivityNotFound() {
        given(activityRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> activityService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
    }

    @Test
    void complete_openActivity_transitionsToCompleted() {
        Activity activity = buildActivity();
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

        ActivityResponse result = activityService.complete(1L);

        assertThat(result.status()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void cancel_openActivity_transitionsToCancelled() {
        Activity activity = buildActivity();
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

        ActivityResponse result = activityService.cancel(1L);

        assertThat(result.status()).isEqualTo(ActivityStatus.CANCELLED);
    }

    @Test
    void complete_alreadyCompleted_throwsInvalidStatusTransition() {
        Activity activity = buildActivity();
        activity.complete();
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

        ErpException ex = assertThrows(ErpException.class, () -> activityService.complete(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
    }

    @Test
    void cancel_alreadyCancelled_throwsInvalidStatusTransition() {
        Activity activity = buildActivity();
        activity.cancel();
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

        ErpException ex = assertThrows(ErpException.class, () -> activityService.cancel(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
    }
}
