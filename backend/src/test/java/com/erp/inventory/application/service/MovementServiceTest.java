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

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock private MovementRepository movementRepository;
    @Mock private MovementLineRepository movementLineRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ItemService itemService;
    @Mock private LocationService locationService;

    @InjectMocks private MovementService movementService;

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
}
