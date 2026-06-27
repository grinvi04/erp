package com.erp.inventory.application.service;

import com.erp.inventory.application.ReferenceTypes;
import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStep;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.inventory.application.dto.MovementCreateRequest;
import com.erp.inventory.application.dto.MovementLineRequest;
import com.erp.inventory.application.dto.MovementLineResponse;
import com.erp.inventory.application.dto.MovementResponse;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementLine;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.Stock;
import com.erp.inventory.domain.repository.MovementLineRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import com.erp.inventory.domain.repository.StockRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovementService {

    private static final int MOVEMENT_NO_MAX_RETRIES = 10;
    private static final int MOVEMENT_NO_SUFFIX_MIN = 10000;
    private static final int MOVEMENT_NO_SUFFIX_MAX = 99999;

    private final MovementRepository movementRepository;
    private final MovementLineRepository movementLineRepository;
    private final StockRepository stockRepository;
    private final ItemService itemService;
    private final LocationService locationService;
    private final PermissionChecker permissionChecker;
    private final CurrentUserProvider currentUserProvider;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditService auditService;

    // 결재선에 특정인을 사전 지정하지 않는다 — 결재함이 권한(role)으로 결재자를 산출한다(역할 sentinel).
    private static final String ROLE_BASED_APPROVER = "@role:" + Permission.INVENTORY_MOVEMENT_APPROVE;

    public PageResponse<MovementResponse> findAll(MovementType type, MovementStatus status, Pageable pageable) {
        permissionChecker.require(Permission.INVENTORY_READ);
        var page = movementRepository.findByTypeAndStatus(type, status, pageable);
        if (page.isEmpty()) {
            return PageResponse.from(page.map(MovementResponse::from));
        }
        List<Long> ids = page.getContent().stream().map(Movement::getId).toList();
        Map<Long, List<MovementLineResponse>> linesMap = movementLineRepository
                .findByMovement_IdInOrderByLineNoAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        l -> l.getMovement().getId(),
                        Collectors.mapping(MovementLineResponse::from, Collectors.toList())));
        return PageResponse.from(page.map(m ->
                MovementResponse.from(m, linesMap.getOrDefault(m.getId(), List.of()))));
    }

    public MovementResponse findById(Long id) {
        permissionChecker.require(Permission.INVENTORY_READ);
        Movement movement = getOrThrow(id);
        return toResponse(id, movement);
    }

    @Transactional
    public MovementResponse create(MovementCreateRequest req) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        String movementNo = generateMovementNo();
        Movement movement;
        try {
            movement = movementRepository.saveAndFlush(
                    Movement.of(movementNo, req.movementType(), req.movementDate(),
                            req.referenceType(), req.referenceId(), req.note()));
        } catch (DataIntegrityViolationException e) {
            throw new ErpException(ErrorCode.INTERNAL_ERROR);
        }

        List<MovementLine> lines = buildLines(movement, req);
        movementLineRepository.saveAll(lines);

        List<MovementLineResponse> lineResponses = lines.stream().map(MovementLineResponse::from).toList();
        return MovementResponse.from(movement, lineResponses);
    }

    @Transactional
    public MovementResponse confirm(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        Movement movement = getOrThrow(id);
        movement.confirm();
        List<MovementLine> lines = applyStockEffects(id);
        log.atInfo().addKeyValue("event", "STOCK_MOVEMENT_CONFIRMED")
                .addKeyValue("movementId", movement.getId())
                .addKeyValue("movementNo", movement.getMovementNo())
                .addKeyValue("movementType", movement.getMovementType())
                .log("재고 이동 확정");
        return MovementResponse.from(movement, lines.stream().map(MovementLineResponse::from).toList());
    }

    /**
     * 재고 조정 결재 상신: ADJUSTMENT DRAFT → PENDING_APPROVAL + ApprovalRequest(STOCK_MOVEMENT) 생성·링크.
     * 조정 이동만 결재 대상이며(도메인 가드), 결재함 라우팅은 {@link StockMovementApprovalInboxContributor}가
     * 권한(역할 sentinel)으로 산출한다.
     */
    @Transactional
    public MovementResponse submitForApproval(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        Movement movement = getOrThrow(id);
        movement.submitForApproval();
        ApprovalStep step = ApprovalStep.of(1, "재고 조정 이동 승인", ROLE_BASED_APPROVER);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
                ReferenceTypes.STOCK_MOVEMENT, movement.getId(),
                "재고 조정 이동 승인: " + movement.getMovementNo(),
                userId, new ArrayList<>(List.of(step)));
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        movement.linkApprovalRequest(saved.getId());
        log.atInfo().addKeyValue("event", "STOCK_MOVEMENT_SUBMITTED")
                .addKeyValue("movementId", movement.getId())
                .addKeyValue("movementNo", movement.getMovementNo())
                .log("재고 조정 이동 결재 상신");
        return toResponse(id, movement);
    }

    /**
     * 재고 조정 확정 결재: 결재권(inventory:movement:approve) 보유자만, 작성자≠결재자 충족 시
     * ApprovalRequest 승인 후 PENDING_APPROVAL → CONFIRMED + 재고 증감 반영. 작성권(inventory:write)과 분리.
     */
    @Transactional
    public MovementResponse approve(Long id) {
        permissionChecker.require(Permission.INVENTORY_MOVEMENT_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        Movement movement = getOrThrow(id);
        // 직무분리: 본인이 작성한 조정 이동은 확정 결재할 수 없다.
        if (userId.equals(movement.getCreatedBy())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        movement.confirmApproved();
        List<MovementLine> lines = applyStockEffects(id);
        if (movement.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                    .findById(movement.getApprovalRequestId())
                    .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.approve(userId, null);
        }
        auditService.record(ReferenceTypes.STOCK_MOVEMENT, movement.getId(), AuditLog.AuditAction.APPROVE, null, null);
        log.atInfo().addKeyValue("event", "STOCK_MOVEMENT_APPROVED")
                .addKeyValue("movementId", movement.getId())
                .addKeyValue("movementNo", movement.getMovementNo())
                .log("재고 조정 이동 결재 승인");
        return MovementResponse.from(movement, lines.stream().map(MovementLineResponse::from).toList());
    }

    // 확정된 이동의 재고 증감 반영 — 직접 확정(confirm)·결재 승인(approve)이 공유한다.
    private List<MovementLine> applyStockEffects(Long movementId) {
        List<MovementLine> lines = movementLineRepository.findByMovement_IdOrderByLineNoAsc(movementId);
        for (MovementLine line : lines) {
            if (line.getFromLocation() != null) {
                Stock stock = stockRepository.findByItemAndLocationAndLotNoAndSerialNo(
                        line.getItem(), line.getFromLocation(), line.getLotNo(), line.getSerialNo())
                        .orElseThrow(() -> new ErpException(ErrorCode.INSUFFICIENT_STOCK));
                stock.decrease(line.getQty());
            }
            if (line.getToLocation() != null) {
                Stock stock = stockRepository.findByItemAndLocationAndLotNoAndSerialNo(
                        line.getItem(), line.getToLocation(), line.getLotNo(), line.getSerialNo())
                        .orElseGet(() -> Stock.create(
                                line.getItem(), line.getToLocation(),
                                line.getLotNo(), line.getSerialNo(), line.getUnitCost()));
                stock.increase(line.getQty(), line.getUnitCost());
                stockRepository.save(stock);
            }
        }
        return lines;
    }

    @Transactional
    public MovementResponse cancel(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        Movement movement = getOrThrow(id);
        movement.cancel();
        return toResponse(id, movement);
    }

    private List<MovementLine> buildLines(Movement movement, MovementCreateRequest req) {
        MovementType movementType = req.movementType();
        List<MovementLineRequest> lineReqs = req.lines();
        List<MovementLine> result = new ArrayList<>(lineReqs.size());
        for (int i = 0; i < lineReqs.size(); i++) {
            MovementLineRequest lineReq = lineReqs.get(i);
            Item item = itemService.getOrThrow(lineReq.itemId());
            if (item.isLotTracked() && lineReq.lotNo() == null) {
                throw new ErpException(ErrorCode.LOT_NO_REQUIRED);
            }
            if (item.isSerialTracked() && lineReq.serialNo() == null) {
                throw new ErpException(ErrorCode.SERIAL_NO_REQUIRED);
            }
            Location fromLocation = lineReq.fromLocationId() != null
                    ? locationService.getOrThrow(lineReq.fromLocationId()) : null;
            Location toLocation = lineReq.toLocationId() != null
                    ? locationService.getOrThrow(lineReq.toLocationId()) : null;
            validateLocationForType(movementType, fromLocation, toLocation);
            result.add(MovementLine.of(movement, i + 1, item, fromLocation, toLocation,
                    lineReq.lotNo(), lineReq.serialNo(), lineReq.qty(), lineReq.unitCost()));
        }
        return result;
    }

    private void validateLocationForType(MovementType type, Location from, Location to) {
        if (type == MovementType.RECEIPT && to == null) {
            throw new ErpException(ErrorCode.LOCATION_REQUIRED);
        }
        if (type == MovementType.ISSUE && from == null) {
            throw new ErpException(ErrorCode.LOCATION_REQUIRED);
        }
        if (type == MovementType.TRANSFER && (from == null || to == null)) {
            throw new ErpException(ErrorCode.LOCATION_REQUIRED);
        }
    }

    private String generateMovementNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < MOVEMENT_NO_MAX_RETRIES; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(MOVEMENT_NO_SUFFIX_MIN, MOVEMENT_NO_SUFFIX_MAX + 1);
            String no = "MOV-" + date + "-" + suffix;
            if (!movementRepository.existsByMovementNo(no)) {
                return no;
            }
        }
        throw new ErpException(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 재고 조정 결재 반려: 결재권(inventory:movement:approve) 보유자만, 작성자≠결재자 충족 시 반려 사유와 함께
     * ApprovalRequest를 REJECTED로 종료하고 이동을 PENDING_APPROVAL → DRAFT로 되돌린다(수정·재상신 가능).
     */
    @Transactional
    public MovementResponse reject(Long id, String comment) {
        permissionChecker.require(Permission.INVENTORY_MOVEMENT_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        if (comment == null || comment.isBlank()) {
            throw new ErpException(ErrorCode.INVALID_INPUT);
        }
        Movement movement = getOrThrow(id);
        // 직무분리: 본인이 작성한 조정 이동은 반려(결재 행위)할 수 없다 — 철회(withdraw)로 처리한다.
        if (userId.equals(movement.getCreatedBy())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        movement.returnToDraft();
        if (movement.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                    .findById(movement.getApprovalRequestId())
                    .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.reject(userId, comment);
        }
        auditService.record(ReferenceTypes.STOCK_MOVEMENT, movement.getId(), AuditLog.AuditAction.REJECT, null, null);
        log.atInfo().addKeyValue("event", "STOCK_MOVEMENT_REJECTED")
                .addKeyValue("movementId", movement.getId())
                .addKeyValue("movementNo", movement.getMovementNo())
                .log("재고 조정 이동 결재 반려");
        return toResponse(id, movement);
    }

    /**
     * 재고 조정 결재 철회: 상신자 본인(작성권 inventory:write)만, ApprovalRequest를 CANCELLED로 종료하고
     * 이동을 PENDING_APPROVAL → DRAFT로 되돌린다(수정·재상신 가능). 타인은 철회할 수 없다.
     */
    @Transactional
    public MovementResponse withdraw(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        Movement movement = getOrThrow(id);
        // 상신자 본인만 철회 가능.
        if (userId == null || !userId.equals(movement.getCreatedBy())) {
            throw new ErpException(ErrorCode.FORBIDDEN);
        }
        movement.returnToDraft();
        if (movement.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                    .findById(movement.getApprovalRequestId())
                    .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.cancel(userId, null);
        }
        auditService.record(ReferenceTypes.STOCK_MOVEMENT, movement.getId(), AuditLog.AuditAction.WITHDRAW, null, null);
        log.atInfo().addKeyValue("event", "STOCK_MOVEMENT_WITHDRAWN")
                .addKeyValue("movementId", movement.getId())
                .addKeyValue("movementNo", movement.getMovementNo())
                .log("재고 조정 이동 결재 철회");
        return toResponse(id, movement);
    }

    private MovementResponse toResponse(Long id, Movement movement) {
        List<MovementLineResponse> lines = movementLineRepository
                .findByMovement_IdOrderByLineNoAsc(id).stream()
                .map(MovementLineResponse::from).toList();
        return MovementResponse.from(movement, lines);
    }

    private Movement getOrThrow(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.MOVEMENT_NOT_FOUND));
    }
}
