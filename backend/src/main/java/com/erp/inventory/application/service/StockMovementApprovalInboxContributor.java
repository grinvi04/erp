package com.erp.inventory.application.service;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.PendingApprovalContributor;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.inventory.application.ReferenceTypes;
import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.repository.MovementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 조정(ADJUSTMENT) 이동 확정 결재를 통합 결재함에 기여한다. 결재권(inventory:movement:approve) 보유자에게 본인 작성 아닌
 * 대기(PENDING_APPROVAL) 조정 이동을 보여준다 — {@link MovementService#approve}의 결재 권한 기준과 동일하게 산출한다. 재고는 금액
 * 전결한도 미적용.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockMovementApprovalInboxContributor implements PendingApprovalContributor {

  private final MovementRepository movementRepository;
  private final CurrentUserProvider currentUserProvider;
  private final PermissionChecker permissionChecker;

  @Override
  public List<ApprovalSummaryResponse> pendingForCurrentUser() {
    String userId = currentUserProvider.getCurrentUserId();
    if (userId == null || !permissionChecker.hasPermission(Permission.INVENTORY_MOVEMENT_APPROVE)) {
      return List.of();
    }
    return movementRepository
        .findPendingApprovableBy(MovementStatus.PENDING_APPROVAL, userId)
        .stream()
        .map(this::toSummary)
        .toList();
  }

  private ApprovalSummaryResponse toSummary(Movement m) {
    return new ApprovalSummaryResponse(
        m.getApprovalRequestId(),
        ReferenceTypes.STOCK_MOVEMENT,
        m.getId(),
        "재고 조정 이동 승인: " + m.getMovementNo(),
        ApprovalStatus.PENDING,
        m.getCreatedBy(),
        1,
        1,
        "재고 조정 이동 승인",
        null,
        m.getCreatedAt(),
        null);
  }
}
