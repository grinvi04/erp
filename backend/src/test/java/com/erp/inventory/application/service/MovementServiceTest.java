package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.MovementCreateRequest;
import com.erp.inventory.application.dto.MovementLineRequest;
import com.erp.inventory.application.dto.MovementResponse;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementLine;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.Stock;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.MovementLineRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import com.erp.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock private MovementRepository movementRepository;
    @Mock private MovementLineRepository movementLineRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ItemService itemService;
    @Mock private LocationService locationService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private com.erp.common.security.CurrentUserProvider currentUserProvider;
    @Mock private com.erp.common.workflow.repository.ApprovalRequestRepository approvalRequestRepository;
    @Mock private com.erp.common.audit.AuditService auditService;

    @InjectMocks private MovementService movementService;

    private Movement buildAdjustmentMovement() {
        return Movement.of("MOV-20260624-44444", MovementType.ADJUSTMENT,
                LocalDate.of(2026, 6, 24), null, null, "조정");
    }

    private Item buildItem() {
        return Item.of("SKU-001", "테스트품목", null, null, UnitOfMeasure.of("EA", "개"),
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
    }

    private Location buildLocation() {
        return Location.of(Warehouse.of("WH-001", "본창고", "서울"), "A-01", "구역A", null, LocationType.ZONE);
    }

    private Movement buildDraftMovement() {
        return Movement.of("MOV-20260624-11111", MovementType.RECEIPT,
                LocalDate.of(2026, 6, 24), null, null, "입고");
    }

    @Test
    void create_receiptMovement_savesMovementAndLines() {
        Item item = buildItem();
        Location loc = buildLocation();
        Movement movement = buildDraftMovement();
        MovementLine line = MovementLine.of(movement, 1, item, null, loc, null, null,
                BigDecimal.TEN, BigDecimal.ONE);

        given(movementRepository.existsByMovementNo(anyString())).willReturn(false);
        given(movementRepository.saveAndFlush(any())).willReturn(movement);
        given(itemService.getOrThrow(1L)).willReturn(item);
        given(locationService.getOrThrow(2L)).willReturn(loc);
        given(movementLineRepository.saveAll(anyList())).willReturn(List.of(line));

        MovementCreateRequest req = new MovementCreateRequest(
                MovementType.RECEIPT, LocalDate.of(2026, 6, 24), null, null, "입고",
                List.of(new MovementLineRequest(1L, null, 2L, null, null, BigDecimal.TEN, BigDecimal.ONE)));

        MovementResponse result = movementService.create(req);

        assertThat(result.movementType()).isEqualTo(MovementType.RECEIPT);
        assertThat(result.status()).isEqualTo(MovementStatus.DRAFT);
        assertThat(result.lines()).hasSize(1);
    }

    @Test
    void confirm_receiptMovement_createsNewStock() {
        Item item = buildItem();
        Location toLoc = buildLocation();
        Movement movement = buildDraftMovement();
        MovementLine line = MovementLine.of(movement, 1, item, null, toLoc, null, null,
                BigDecimal.TEN, BigDecimal.ONE);

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of(line));
        given(stockRepository.findByItemAndLocationAndLotNoAndSerialNo(item, toLoc, null, null))
                .willReturn(Optional.empty());
        given(stockRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        MovementResponse result = movementService.confirm(1L);

        assertThat(result.status()).isEqualTo(MovementStatus.CONFIRMED);
    }

    @Test
    void confirm_issueMovement_decreasesExistingStock() {
        Item item = buildItem();
        Location fromLoc = buildLocation();
        Movement movement = Movement.of("MOV-20260624-22222", MovementType.ISSUE,
                LocalDate.of(2026, 6, 24), null, null, "출고");
        MovementLine line = MovementLine.of(movement, 1, item, fromLoc, null, null, null,
                BigDecimal.TEN, BigDecimal.ONE);

        Stock stock = Stock.create(item, fromLoc, null, null, BigDecimal.ONE);
        stock.increase(new BigDecimal("20"), BigDecimal.ONE);

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of(line));
        given(stockRepository.findByItemAndLocationAndLotNoAndSerialNo(item, fromLoc, null, null))
                .willReturn(Optional.of(stock));

        movementService.confirm(1L);

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void confirm_insufficientStock_throwsInsufficientStock() {
        Item item = buildItem();
        Location fromLoc = buildLocation();
        Movement movement = Movement.of("MOV-20260624-33333", MovementType.ISSUE,
                LocalDate.of(2026, 6, 24), null, null, "출고");
        MovementLine line = MovementLine.of(movement, 1, item, fromLoc, null, null, null,
                new BigDecimal("50"), BigDecimal.ONE);

        Stock stock = Stock.create(item, fromLoc, null, null, BigDecimal.ONE);
        stock.increase(BigDecimal.TEN, BigDecimal.ONE);

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of(line));
        given(stockRepository.findByItemAndLocationAndLotNoAndSerialNo(item, fromLoc, null, null))
                .willReturn(Optional.of(stock));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.confirm(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void confirm_alreadyConfirmed_throwsMovementNotDraft() {
        Movement movement = buildDraftMovement();
        movement.confirm();

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.confirm(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_NOT_DRAFT);
    }

    @Test
    void cancel_draftMovement_setsCancelled() {
        Movement movement = buildDraftMovement();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of());

        MovementResponse result = movementService.cancel(1L);

        assertThat(result.status()).isEqualTo(MovementStatus.CANCELLED);
    }

    @Test
    void findById_notFound_throwsMovementNotFound() {
        given(movementRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> movementService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_NOT_FOUND);
    }

    @Test
    void create_serialTrackedItemWithoutSerialNo_throwsSerialNoRequired() {
        Item serialItem = Item.of("SKU-SN1", "시리얼품목", null, null, UnitOfMeasure.of("EA", "개"),
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, true);
        Movement movement = buildDraftMovement();

        given(movementRepository.existsByMovementNo(anyString())).willReturn(false);
        given(movementRepository.saveAndFlush(any())).willReturn(movement);
        given(itemService.getOrThrow(1L)).willReturn(serialItem);

        MovementCreateRequest req = new MovementCreateRequest(
                MovementType.RECEIPT, LocalDate.of(2026, 6, 24), null, null, "입고",
                List.of(new MovementLineRequest(1L, null, 2L, null, null, BigDecimal.TEN, BigDecimal.ONE)));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.create(req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERIAL_NO_REQUIRED);
    }

    @Test
    void create_lotTrackedItemWithoutLotNo_throwsLotNoRequired() {
        Item lotItem = Item.of("SKU-LOT1", "로트품목", null, null, UnitOfMeasure.of("EA", "개"),
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, false);
        Movement movement = buildDraftMovement();

        given(movementRepository.existsByMovementNo(anyString())).willReturn(false);
        given(movementRepository.saveAndFlush(any())).willReturn(movement);
        given(itemService.getOrThrow(1L)).willReturn(lotItem);

        MovementCreateRequest req = new MovementCreateRequest(
                MovementType.RECEIPT, LocalDate.of(2026, 6, 24), null, null, "입고",
                List.of(new MovementLineRequest(1L, null, 2L, null, null, BigDecimal.TEN, BigDecimal.ONE)));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.create(req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOT_NO_REQUIRED);
    }

    // AC-9: ADJUSTMENT DRAFT 상신 → PENDING_APPROVAL + ApprovalRequest(STOCK_MOVEMENT) 생성·링크
    @Test
    void submitForApproval_adjustmentDraft_transitionsToPendingApprovalAndCreatesApprovalRequest() {
        Movement movement = buildAdjustmentMovement();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of());
        com.erp.common.workflow.ApprovalRequest saved =
                com.erp.common.workflow.ApprovalRequest.create("STOCK_MOVEMENT", 1L, "t", "creator",
                        new java.util.ArrayList<>(List.of(
                                com.erp.common.workflow.ApprovalStep.of(1, "재고 조정 이동 승인",
                                        "@role:inventory:movement:approve"))));
        given(approvalRequestRepository.save(any())).willReturn(saved);

        MovementResponse result = movementService.submitForApproval(1L);

        assertThat(result.status()).isEqualTo(MovementStatus.PENDING_APPROVAL);
        verify(approvalRequestRepository).save(any());
        verify(permissionChecker).require(com.erp.common.security.Permission.INVENTORY_WRITE);
    }

    // AC-13: 비-ADJUSTMENT 이동 상신 거부 (결재 미적용)
    @Test
    void submitForApproval_nonAdjustment_throwsApprovalNotApplicable() {
        Movement movement = buildDraftMovement(); // RECEIPT
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.submitForApproval(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_APPROVAL_NOT_APPLICABLE);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.DRAFT);
    }

    // AC-10: PENDING_APPROVAL ADJUSTMENT 승인 → CONFIRMED + 재고 증감 반영
    @Test
    void approve_pendingAdjustment_transitionsToConfirmedAndAppliesStock() {
        Item item = buildItem();
        Location toLoc = buildLocation();
        Movement movement = buildAdjustmentMovement();
        movement.submitForApproval(); // DRAFT → PENDING_APPROVAL
        MovementLine line = MovementLine.of(movement, 1, item, null, toLoc, null, null,
                BigDecimal.TEN, BigDecimal.ONE);

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of(line));
        given(stockRepository.findByItemAndLocationAndLotNoAndSerialNo(item, toLoc, null, null))
                .willReturn(Optional.empty());
        given(stockRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        MovementResponse result = movementService.approve(1L);

        assertThat(result.status()).isEqualTo(MovementStatus.CONFIRMED);
        verify(permissionChecker).require(com.erp.common.security.Permission.INVENTORY_MOVEMENT_APPROVE);
        verify(stockRepository).save(any());
    }

    // AC-13: 직무분리 — 작성자 본인은 자신의 조정 이동을 결재할 수 없다
    @Test
    void approve_approverIsCreator_throwsApproverNotAuthorized() {
        Movement movement = buildAdjustmentMovement();
        movement.submitForApproval();
        org.springframework.test.util.ReflectionTestUtils.setField(movement, "createdBy", "creator");

        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.approve(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
    }

    // AC-12: ADJUSTMENT 직접 확정 차단 — 결재 경유 강제
    @Test
    void confirm_adjustment_throwsRequiresApproval() {
        Movement movement = buildAdjustmentMovement();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));

        ErpException ex = assertThrows(ErpException.class, () -> movementService.confirm(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_REQUIRES_APPROVAL);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.DRAFT);
    }

    // --- 반려(reject) ---

    private com.erp.common.workflow.ApprovalRequest buildApprovalRequest() {
        return com.erp.common.workflow.ApprovalRequest.create("STOCK_MOVEMENT", 1L, "t", "creator",
            new java.util.ArrayList<>(List.of(
                com.erp.common.workflow.ApprovalStep.of(1, "재고 조정 이동 승인",
                    "@role:inventory:movement:approve"))));
    }

    private Movement buildPendingAdjustment() {
        Movement movement = buildAdjustmentMovement();
        org.springframework.test.util.ReflectionTestUtils.setField(movement, "createdBy", "creator");
        movement.submitForApproval();
        movement.linkApprovalRequest(10L);
        return movement;
    }

    // 결재자 반려 → DRAFT 복귀 + ApprovalRequest REJECTED(사유 저장)
    @Test
    void reject_pendingAdjustment_returnsToDraftAndMarksApprovalRejected() {
        Movement movement = buildPendingAdjustment();
        com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");
        given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of());

        MovementResponse result = movementService.reject(1L, "수량 오류");

        assertThat(result.status()).isEqualTo(MovementStatus.DRAFT);
        assertThat(req.getStatus()).isEqualTo(com.erp.common.workflow.ApprovalStatus.REJECTED);
        assertThat(req.getSteps().get(0).getComment()).isEqualTo("수량 오류");
        verify(permissionChecker).require(com.erp.common.security.Permission.INVENTORY_MOVEMENT_APPROVE);
    }

    // 작성자 본인 반려 차단
    @Test
    void reject_approverIsCreator_throwsApproverNotAuthorized() {
        Movement movement = buildPendingAdjustment();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.reject(1L, "사유"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
    }

    // 권한 없음 → 403 FORBIDDEN
    @Test
    void reject_noApprovePermission_throwsForbidden() {
        org.mockito.BDDMockito.willThrow(new ErpException(ErrorCode.FORBIDDEN))
            .given(permissionChecker).require(com.erp.common.security.Permission.INVENTORY_MOVEMENT_APPROVE);

        ErpException ex = assertThrows(ErpException.class, () -> movementService.reject(1L, "사유"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    // PENDING_APPROVAL 아닌 상태(DRAFT) 반려 거부
    @Test
    void reject_notPendingApproval_throwsNotPendingApproval() {
        Movement movement = buildAdjustmentMovement();
        org.springframework.test.util.ReflectionTestUtils.setField(movement, "createdBy", "creator");
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.reject(1L, "사유"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_NOT_PENDING_APPROVAL);
    }

    // 반려 사유 필수
    @Test
    void reject_blankComment_throwsInvalidInput() {
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.reject(1L, "  "));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
    }

    // --- 철회(withdraw) ---

    // 상신자 본인 철회 → DRAFT + ApprovalRequest CANCELLED
    @Test
    void withdraw_bySubmitter_returnsToDraftAndCancelsApproval() {
        Movement movement = buildPendingAdjustment();
        com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");
        given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of());

        MovementResponse result = movementService.withdraw(1L);

        assertThat(result.status()).isEqualTo(MovementStatus.DRAFT);
        assertThat(req.getStatus()).isEqualTo(com.erp.common.workflow.ApprovalStatus.CANCELLED);
        verify(permissionChecker).require(com.erp.common.security.Permission.INVENTORY_WRITE);
        verify(auditService).record("STOCK_MOVEMENT", movement.getId(),
            com.erp.common.audit.AuditLog.AuditAction.WITHDRAW, null, null);
    }

    // 타인 철회 차단
    @Test
    void withdraw_byNonSubmitter_throwsForbidden() {
        Movement movement = buildPendingAdjustment();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("someone-else");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.withdraw(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
    }

    // PENDING_APPROVAL 아닌 상태 철회 거부
    @Test
    void withdraw_notPendingApproval_throwsNotPendingApproval() {
        Movement movement = buildAdjustmentMovement();
        org.springframework.test.util.ReflectionTestUtils.setField(movement, "createdBy", "creator");
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");

        ErpException ex = assertThrows(ErpException.class, () -> movementService.withdraw(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVEMENT_NOT_PENDING_APPROVAL);
    }

    // 되돌린 DRAFT 재상신 가능
    @Test
    void withdrawnAdjustment_canBeResubmitted() {
        Movement movement = buildPendingAdjustment();
        com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
        given(movementRepository.findById(1L)).willReturn(Optional.of(movement));
        given(currentUserProvider.getCurrentUserId()).willReturn("creator");
        given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));
        given(movementLineRepository.findByMovement_IdOrderByLineNoAsc(1L)).willReturn(List.of());

        movementService.withdraw(1L);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.DRAFT);

        given(approvalRequestRepository.save(any())).willReturn(buildApprovalRequest());
        MovementResponse resubmitted = movementService.submitForApproval(1L);

        assertThat(resubmitted.status()).isEqualTo(MovementStatus.PENDING_APPROVAL);
    }
}
