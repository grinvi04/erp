package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.ActivityCreateRequest;
import com.erp.crm.application.dto.ActivityResponse;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.Activity;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
import com.erp.crm.domain.model.Contact;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final CrmAccountService accountService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final PermissionChecker permissionChecker;
    private final CrmDataScopeResolver dataScopeResolver;
    private final CurrentUserProvider currentUserProvider;

    public PageResponse<ActivityResponse> search(Long opportunityId, Long accountId,
                                                 ActivityType activityType, ActivityStatus status,
                                                 Pageable pageable) {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();
        return PageResponse.from(activityRepository
                .search(opportunityId, accountId, activityType, status,
                        s.scoped(), s.ownerIds(), pageable)
                .map(ActivityResponse::from));
    }

    public ActivityResponse findById(Long id) {
        permissionChecker.require(Permission.CRM_READ);
        var activity = getOrThrow(id);
        dataScopeResolver.requireOwnerAccess(activity.getOwnerId());
        return ActivityResponse.from(activity);
    }

    @Transactional
    public ActivityResponse create(ActivityCreateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        Account account = req.accountId() != null ? accountService.getOrThrow(req.accountId()) : null;
        Contact contact = req.contactId() != null ? contactService.getOrThrow(req.contactId()) : null;
        Opportunity opportunity = req.opportunityId() != null
                ? opportunityService.getOrThrow(req.opportunityId()) : null;

        Activity activity = Activity.of(req.activityType(), req.subject(), account, contact,
                opportunity, currentUserProvider.getCurrentUserId(), req.dueDate(), req.description());
        return ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional
    public ActivityResponse complete(Long id) {
        permissionChecker.require(Permission.CRM_WRITE);
        Activity activity = getOrThrow(id);
        if (!activity.isOpen()) {
            throw new ErpException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }
        activity.complete();
        return ActivityResponse.from(activity);
    }

    @Transactional
    public ActivityResponse cancel(Long id) {
        permissionChecker.require(Permission.CRM_WRITE);
        Activity activity = getOrThrow(id);
        if (!activity.isOpen()) {
            throw new ErpException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }
        activity.cancel();
        return ActivityResponse.from(activity);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.require(Permission.CRM_WRITE);
        getOrThrow(id).softDelete();
    }

    private Activity getOrThrow(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.ACTIVITY_NOT_FOUND));
    }
}
