package com.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.Permission;
import com.erp.inventory.application.dto.MovementCreateRequest;
import com.erp.inventory.application.dto.MovementLineRequest;
import com.erp.inventory.application.dto.MovementResponse;
import com.erp.inventory.application.service.MovementService;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.Stock;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.LocationRepository;
import com.erp.inventory.domain.repository.StockRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import com.erp.inventory.domain.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 창고간(TRANSFER) 이동이 출고 창고/위치와 입고 창고/위치를 서로 다른 창고로 지정해 생성·확정될 때 양 창고의 재고가 올바르게 반영되는지(출고 차감·입고 증가),
 * 그리고 로트 추적 품목 이동이 lotNo를 동반하면 거부 없이 처리되는지 검증한다.
 */
@Transactional
class MovementCrossWarehouseTransferIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MovementService movementService;
  @Autowired private ItemRepository itemRepository;
  @Autowired private ItemCategoryRepository itemCategoryRepository;
  @Autowired private UnitOfMeasureRepository uomRepository;
  @Autowired private WarehouseRepository warehouseRepository;
  @Autowired private LocationRepository locationRepository;
  @Autowired private StockRepository stockRepository;

  private Item newItem(String sku, boolean lotTracked) {
    ItemCategory cat = itemCategoryRepository.save(ItemCategory.of("CAT-" + sku, "분류", null));
    UnitOfMeasure uom = uomRepository.save(UnitOfMeasure.of("UOM-" + sku, "Each"));
    return itemRepository.save(
        Item.of(
            sku,
            "품목",
            null,
            cat,
            uom,
            CostMethod.WEIGHTED_AVG,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.valueOf(1000),
            lotTracked,
            false));
  }

  private Location newLocation(String whCode, String locCode) {
    Warehouse wh = warehouseRepository.save(Warehouse.of(whCode, "창고", null));
    return locationRepository.save(Location.of(wh, locCode, "위치", null, LocationType.BIN));
  }

  private void seedStock(Item item, Location loc, String lotNo, BigDecimal qty) {
    Stock stock = Stock.create(item, loc, lotNo, null, BigDecimal.ONE);
    stock.increase(qty, BigDecimal.ONE);
    stockRepository.saveAndFlush(stock);
  }

  @Test
  void transfer_acrossWarehouses_movesStockBetweenLocations() {
    authenticate("tester", Permission.INVENTORY_WRITE);
    Item item = newItem("SKU-XW", false);
    Location from = newLocation("WH-XW-A", "LOC-XW-A");
    Location to = newLocation("WH-XW-B", "LOC-XW-B");
    seedStock(item, from, null, BigDecimal.valueOf(100));

    MovementResponse created =
        movementService.create(
            new MovementCreateRequest(
                MovementType.TRANSFER,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(
                    new MovementLineRequest(
                        item.getId(),
                        from.getId(),
                        to.getId(),
                        null,
                        null,
                        BigDecimal.valueOf(30),
                        BigDecimal.ONE))));

    movementService.confirm(created.id());

    BigDecimal fromQty =
        stockRepository
            .findByItemAndLocationAndLotNoAndSerialNo(item, from, null, null)
            .orElseThrow()
            .getQtyOnHand();
    BigDecimal toQty =
        stockRepository
            .findByItemAndLocationAndLotNoAndSerialNo(item, to, null, null)
            .orElseThrow()
            .getQtyOnHand();
    assertThat(fromQty).isEqualByComparingTo("70");
    assertThat(toQty).isEqualByComparingTo("30");
  }

  @Test
  void transfer_lotTrackedItem_succeedsWithLotNo() {
    authenticate("tester", Permission.INVENTORY_WRITE);
    Item item = newItem("SKU-LOT", true);
    Location from = newLocation("WH-LOT-A", "LOC-LOT-A");
    Location to = newLocation("WH-LOT-B", "LOC-LOT-B");
    seedStock(item, from, "LOT-1", BigDecimal.valueOf(50));

    MovementResponse created =
        movementService.create(
            new MovementCreateRequest(
                MovementType.TRANSFER,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(
                    new MovementLineRequest(
                        item.getId(),
                        from.getId(),
                        to.getId(),
                        "LOT-1",
                        null,
                        BigDecimal.valueOf(20),
                        BigDecimal.ONE))));

    movementService.confirm(created.id());

    BigDecimal fromQty =
        stockRepository
            .findByItemAndLocationAndLotNoAndSerialNo(item, from, "LOT-1", null)
            .orElseThrow()
            .getQtyOnHand();
    BigDecimal toQty =
        stockRepository
            .findByItemAndLocationAndLotNoAndSerialNo(item, to, "LOT-1", null)
            .orElseThrow()
            .getQtyOnHand();
    assertThat(fromQty).isEqualByComparingTo("30");
    assertThat(toQty).isEqualByComparingTo("20");
  }
}
