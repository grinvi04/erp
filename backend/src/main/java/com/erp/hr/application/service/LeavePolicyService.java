package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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

    public List<LeavePolicyResponse> findAll() {
        return leavePolicyRepository.findAll().stream()
            .map(LeavePolicyResponse::from)
            .toList();
    }

    public LeavePolicyResponse findById(Long id) {
        return LeavePolicyResponse.from(getOrThrow(id));
    }

    @Transactional
    public LeavePolicyResponse create(LeavePolicyCreateRequest request) {
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
        LeavePolicy policy = getOrThrow(id);
        policy.softDelete();
    }

    private LeavePolicy getOrThrow(Long id) {
        return leavePolicyRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
