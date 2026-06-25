package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<MovementLineResponse> lines = movementLineRepository
                .findByMovement_IdOrderByLineNoAsc(id).stream()
                .map(MovementLineResponse::from).toList();
        return MovementResponse.from(movement, lines);
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

        List<MovementLine> lines = movementLineRepository.findByMovement_IdOrderByLineNoAsc(id);
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

        List<MovementLineResponse> lineResponses = lines.stream().map(MovementLineResponse::from).toList();
        return MovementResponse.from(movement, lineResponses);
    }

    @Transactional
    public MovementResponse cancel(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        Movement movement = getOrThrow(id);
        movement.cancel();
        List<MovementLineResponse> lines = movementLineRepository
                .findByMovement_IdOrderByLineNoAsc(id).stream()
                .map(MovementLineResponse::from).toList();
        return MovementResponse.from(movement, lines);
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

    private Movement getOrThrow(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.MOVEMENT_NOT_FOUND));
    }
}
