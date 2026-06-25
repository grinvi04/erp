package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.LeavePolicyCreateRequest;
import com.erp.hr.application.dto.LeavePolicyResponse;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeavePolicyService {

    private final LeavePolicyRepository leavePolicyRepository;
    private final PermissionChecker permissionChecker;

    public List<LeavePolicyResponse> findAll() {
        permissionChecker.require(Permission.HR_LEAVE_READ);
        return leavePolicyRepository.findAll().stream()
            .map(LeavePolicyResponse::from)
            .toList();
    }

    public LeavePolicyResponse findById(Long id) {
        permissionChecker.require(Permission.HR_LEAVE_READ);
        return LeavePolicyResponse.from(getOrThrow(id));
    }

    @Transactional
    public LeavePolicyResponse create(LeavePolicyCreateRequest request) {
        permissionChecker.require(Permission.HR_LEAVE_WRITE);
        if (leavePolicyRepository.existsByCode(request.code())) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        LeavePolicy policy = LeavePolicy.of(
            request.code(), request.name(), request.leaveType(),
            request.annualDays(), request.carryOverDays(),
            request.requiresApproval(), request.minNoticeDays());
        return LeavePolicyResponse.from(leavePolicyRepository.save(policy));
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.require(Permission.HR_LEAVE_WRITE);
        LeavePolicy policy = getOrThrow(id);
        policy.softDelete();
    }

    private LeavePolicy getOrThrow(Long id) {
        return leavePolicyRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
