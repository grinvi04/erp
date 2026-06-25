package com.erp.common.security.dto;

import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import java.math.BigDecimal;

public record AccessProfileResponse(
    String userId,
    DataScope dataScope,
    Long departmentId,
    BigDecimal approvalLimit
) {
    public static AccessProfileResponse from(UserAccessProfile p) {
        return new AccessProfileResponse(p.getUserId(), p.getDataScope(),
            p.getDepartmentId(), p.getApprovalLimit());
    }
}
