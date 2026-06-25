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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ItemLowStockQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemCategoryRepository itemCategoryRepository;
    @Autowired
    private UnitOfMeasureRepository uomRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private StockRepository stockRepository;

    private Item createItem(String sku, BigDecimal reorderPoint, boolean active) {
        ItemCategory cat = itemCategoryRepository.save(ItemCategory.of("CAT-" + sku, "Category " + sku, null));
        UnitOfMeasure uom = uomRepository.save(UnitOfMeasure.of("EA-" + sku, "Each " + sku));
        Item item = Item.of(sku, "Item " + sku, null, cat, uom,
                CostMethod.WEIGHTED_AVG, BigDecimal.ONE,
                reorderPoint, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.valueOf(1000),
                false, false);
        if (!active) {
            item.deactivate();
        }
        return itemRepository.save(item);
    }

    private Stock createStock(Item item, BigDecimal qty) {
        Warehouse wh = warehouseRepository.save(Warehouse.of("WH-" + item.getSku(), "Warehouse " + item.getSku(), null));
        Location loc = locationRepository.save(Location.of(wh, "LOC-" + item.getSku(), "Location " + item.getSku(), null, LocationType.BIN));
        Stock stock = Stock.create(item, loc, null, null, BigDecimal.ONE);
        stock.increase(qty, BigDecimal.ONE);
        return stockRepository.save(stock);
    }

    @Test
    void countLowStockItems_appliesActiveAndStockThresholdFiltersCorrectly() {
        // Item A: active, reorderPoint=10, stock=5 (below) — COUNTED
        Item itemA = createItem("ITEM-A", BigDecimal.TEN, true);
        createStock(itemA, BigDecimal.valueOf(5));

        // Item B: active, reorderPoint=10, stock=50 (above) — NOT counted
        Item itemB = createItem("ITEM-B", BigDecimal.TEN, true);
        createStock(itemB, BigDecimal.valueOf(50));

        // Item C: active, reorderPoint=10, no stock rows → COALESCE(0) <= 10 — COUNTED
        createItem("ITEM-C", BigDecimal.TEN, true);

        // Item D: INACTIVE, stock=0 → not counted because active=false filter
        Item itemD = createItem("ITEM-D", BigDecimal.TEN, false);
        createStock(itemD, BigDecimal.ONE);

        assertThat(itemRepository.countLowStockItems())
                .as("Items A (qty<reorder) and C (no stock) should be counted as low-stock")
                .isEqualTo(2L);

        assertThat(itemRepository.countByActiveTrue())
                .as("Items A, B, C are active; D is inactive")
                .isEqualTo(3L);
    }

    @Test
    void countLowStockItems_zeroStockIsLowStock_explicitBehavior() {
        // Verify explicitly: an item with no stock rows is treated as low stock (COALESCE 0 <= reorderPoint).
        // This is important: if reorderPoint were 0, the item would NOT be low-stock.
        // Here reorderPoint=10 so zero-stock items are always counted.
        Item zeroStock = createItem("ZERO-STOCK", BigDecimal.TEN, true);

        assertThat(itemRepository.countLowStockItems())
                .as("Active item with no stock rows should register as low-stock via COALESCE(SUM, 0) <= reorderPoint")
                .isEqualTo(1L);

        // Confirm it shows up in the active count too
        assertThat(itemRepository.countByActiveTrue()).isEqualTo(1L);

        // Now add enough stock to push it above the reorder point
        createStock(zeroStock, BigDecimal.valueOf(15));
        assertThat(itemRepository.countLowStockItems())
                .as("After stocking above reorderPoint, item should no longer be low-stock")
                .isEqualTo(0L);
    }

    // Cross-tenant isolation is enforced by the Hibernate @Filter on tenant_id at the DB level.
    // A mid-transaction TenantContext switch within a @Transactional test causes a shared
    // persistence context to flush items for both tenants before the filter re-applies,
    // producing unreliable counts.  Full tenant isolation is therefore verified by the
    // TenantHibernateFilter unit tests (tenant_id predicate is always injected into every query)
    // rather than by an integration test that switches tenants inside one transaction.
}
