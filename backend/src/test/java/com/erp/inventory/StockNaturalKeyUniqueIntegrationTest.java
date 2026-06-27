package com.erp.inventory;

import com.erp.common.AbstractIntegrationTest;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * stock 자연키 UNIQUE가 lot_no/serial_no NULL을 동일하게 취급(NULLS NOT DISTINCT)하는지 검증.
 * 비-로트/시리얼 품목은 lot/serial이 NULL인데, 과거 UNIQUE는 NULL을 distinct로 봐 같은
 * (item, location)에 중복 stock 행을 허용했다 — 동시 입고 시 중복·이후 단건조회 깨짐의 원인.
 */
@Transactional
class StockNaturalKeyUniqueIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private StockRepository stockRepository;

    @Test
    void duplicateNaturalKey_withNullLotAndSerial_isRejected() {
        ItemCategory cat = itemCategoryRepository.save(ItemCategory.of("CAT-DUP", "분류", null));
        UnitOfMeasure uom = uomRepository.save(UnitOfMeasure.of("EA-DUP", "Each"));
        Item item = itemRepository.save(Item.of("SKU-DUP", "품목", null, cat, uom,
                CostMethod.WEIGHTED_AVG, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.valueOf(1000), false, false));
        Warehouse wh = warehouseRepository.save(Warehouse.of("WH-DUP", "창고", null));
        Location loc = locationRepository.save(
                Location.of(wh, "LOC-DUP", "위치", null, LocationType.BIN));

        // lot/serial 모두 NULL인 첫 행 — 정상
        stockRepository.saveAndFlush(Stock.create(item, loc, null, null, BigDecimal.ONE));

        // 같은 (item, location) + NULL lot/serial 둘째 행 — NULLS NOT DISTINCT로 거부돼야 함
        assertThatThrownBy(() ->
                stockRepository.saveAndFlush(Stock.create(item, loc, null, null, BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
