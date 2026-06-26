package com.erp.inventory;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.inventory.application.dto.CategoryItemCountResponse;
import com.erp.inventory.application.dto.LowStockItemResponse;
import com.erp.inventory.application.dto.MonthlyMovementByTypeResponse;
import com.erp.inventory.application.dto.MonthlyMovementResponse;
import com.erp.inventory.application.dto.MovementTypeCountResponse;
import com.erp.inventory.application.dto.WarehouseStockResponse;
import com.erp.inventory.application.service.InventoryAnalyticsService;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
import com.erp.inventory.domain.model.Movement;
import com.erp.inventory.domain.model.MovementLine;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.Stock;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.LocationRepository;
import com.erp.inventory.domain.repository.MovementLineRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import com.erp.inventory.domain.repository.StockRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import com.erp.inventory.domain.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class InventoryAnalyticsIntegrationTest extends AbstractIntegrationTest {

    @Autowired private InventoryAnalyticsService service;
    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private MovementLineRepository movementLineRepository;

    private final AtomicInteger seq = new AtomicInteger();

    private ItemCategory catA;
    private ItemCategory catB;
    private ItemCategory catEmpty;
    private ItemCategory catLow;
    private Warehouse whX;
    private Warehouse whEmpty;
    private Warehouse whInactive;
    private Location locX;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(List<String> permissions) {
        String sub = "user-inv";
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                permissions.stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private void authenticated() {
        authenticate(List.of("inventory:read"));
    }

    private UnitOfMeasure uom() {
        return uomRepository.save(UnitOfMeasure.of("U" + seq.incrementAndGet(), "Unit"));
    }

    private Item item(String sku, ItemCategory category, BigDecimal reorderPoint, boolean active) {
        Item item = Item.of(sku, "Item " + sku, null, category, uom(),
                CostMethod.WEIGHTED_AVG, BigDecimal.ONE,
                reorderPoint, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(1000),
                false, false);
        if (!active) {
            item.deactivate();
        }
        return itemRepository.save(item);
    }

    private Stock stock(Item item, Location location, BigDecimal qty, BigDecimal unitCost) {
        return stock(item, location, null, qty, unitCost);
    }

    private Stock stock(Item item, Location location, String lotNo, BigDecimal qty, BigDecimal unitCost) {
        Stock s = Stock.create(item, location, lotNo, null, unitCost);
        s.increase(qty, unitCost);
        return stockRepository.save(s);
    }

    private Movement confirmedMovement(MovementType type, LocalDate date, BigDecimal... lineQtys) {
        return movement(type, date, true, lineQtys);
    }

    private Movement movement(MovementType type, LocalDate date, boolean confirm, BigDecimal... lineQtys) {
        int n = seq.incrementAndGet();
        Movement m = Movement.of("MV-" + n, type, date, null, null, null);
        movementRepository.save(m);
        int line = 1;
        for (BigDecimal qty : lineQtys) {
            movementLineRepository.save(
                    MovementLine.of(m, line++, item("SKU-MVL-" + n + "-" + line, catA, BigDecimal.ZERO, true),
                            null, locX, null, null, qty, BigDecimal.ONE));
        }
        if (confirm) {
            // ADJUSTMENT는 결재 경유로만 CONFIRMED 도달(직접 confirm 차단) — submit→승인 확정 경로로 시드.
            if (type == MovementType.ADJUSTMENT) {
                m.submitForApproval();
                m.confirmApproved();
            } else {
                m.confirm();
            }
            movementRepository.save(m);
        }
        return m;
    }

    @BeforeEach
    void seed() {
        catA = categoryRepository.save(ItemCategory.of("CAT-A", "원자재", null));
        catB = categoryRepository.save(ItemCategory.of("CAT-B", "완제품", null));
        catEmpty = categoryRepository.save(ItemCategory.of("CAT-E", "빈카테고리", null));
        catLow = categoryRepository.save(ItemCategory.of("CAT-L", "소모품", null));

        // AC-9: catA 2 active, catB 1 active + 1 inactive(제외), catEmpty 0
        item("ITEM-A1", catA, BigDecimal.ZERO, true);
        item("ITEM-A2", catA, BigDecimal.ZERO, true);
        item("ITEM-B1", catB, BigDecimal.ZERO, true);
        item("ITEM-B2", catB, BigDecimal.ZERO, false); // inactive → category count 제외

        // AC-10: whX 재고 있음, whEmpty 활성·재고 없음(0/0), whInactive 비활성(제외)
        whX = warehouseRepository.save(Warehouse.of("WH-X", "본창고", null));
        whEmpty = warehouseRepository.save(Warehouse.of("WH-EMPTY", "빈창고", null));
        whInactive = warehouseRepository.save(Warehouse.of("WH-INACT", "비활성창고", null));
        whInactive.deactivate();
        warehouseRepository.save(whInactive);

        locX = locationRepository.save(Location.of(whX, "LOC-X", "위치X", null, LocationType.BIN));
        // 빈창고에도 location은 두되 stock 없음 → 합계 0 (LEFT JOIN 보존 확인)
        locationRepository.save(Location.of(whEmpty, "LOC-E", "위치E", null, LocationType.BIN));

        Item stockItem = item("ITEM-STK", catA, BigDecimal.ZERO, true);
        // 동일 품목·위치라 자연키 충돌 방지를 위해 lot으로 분리. 창고 합산은 lot 무관.
        stock(stockItem, locX, "LOT-1", BigDecimal.valueOf(10), BigDecimal.valueOf(5)); // value 50
        stock(stockItem, locX, "LOT-2", BigDecimal.valueOf(4), new BigDecimal("2.5"));  // value 10
        // total whX: qty 14, value 60
    }

    // AC-9
    @Test
    void byCategory_countsActiveItems_preservesEmptyCategory() {
        authenticated();
        List<CategoryItemCountResponse> rows = service.getActiveItemsByCategory();
        assertThat(catCount(rows, catA.getId())).isEqualTo(3L); // A1, A2, ITEM-STK
        assertThat(catCount(rows, catB.getId())).isEqualTo(1L); // B1 (B2 inactive 제외)
        assertThat(catCount(rows, catEmpty.getId())).isEqualTo(0L); // 빈 카테고리 보존
    }

    // AC-10
    @Test
    void byWarehouse_sumsQtyAndValue_preservesEmptyActiveWarehouse_excludesInactive() {
        authenticated();
        List<WarehouseStockResponse> rows = service.getStockByWarehouse();

        WarehouseStockResponse x = warehouse(rows, whX.getId());
        assertThat(x.totalQty()).isEqualByComparingTo(BigDecimal.valueOf(14));
        assertThat(x.totalValue()).isEqualByComparingTo(BigDecimal.valueOf(60));

        WarehouseStockResponse empty = warehouse(rows, whEmpty.getId());
        assertThat(empty.totalQty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(rows).extracting(WarehouseStockResponse::warehouseId)
                .doesNotContain(whInactive.getId());
    }

    // AC-11
    @Test
    void movementsByType_countsConfirmedOnly_allEnumValuesPresent() {
        confirmedMovement(MovementType.RECEIPT, LocalDate.of(2026, 1, 10), BigDecimal.TEN);
        confirmedMovement(MovementType.RECEIPT, LocalDate.of(2026, 3, 5), BigDecimal.valueOf(5));
        confirmedMovement(MovementType.ISSUE, LocalDate.of(2026, 1, 20), BigDecimal.valueOf(2));
        confirmedMovement(MovementType.ADJUSTMENT, LocalDate.of(2025, 12, 1), BigDecimal.valueOf(8));
        movement(MovementType.RECEIPT, LocalDate.of(2026, 2, 1), false, BigDecimal.valueOf(99)); // DRAFT 제외
        authenticated();

        List<MovementTypeCountResponse> rows = service.getMovementsByType();
        assertThat(rows).extracting(MovementTypeCountResponse::movementType)
                .containsExactly(MovementType.values());
        assertThat(typeCount(rows, MovementType.RECEIPT)).isEqualTo(2L);
        assertThat(typeCount(rows, MovementType.ISSUE)).isEqualTo(1L);
        assertThat(typeCount(rows, MovementType.ADJUSTMENT)).isEqualTo(1L);
        assertThat(typeCount(rows, MovementType.TRANSFER)).isEqualTo(0L);
        assertThat(typeCount(rows, MovementType.RETURN)).isEqualTo(0L);
    }

    // AC-12
    @Test
    void monthlyMovements_fills12Months_perType_confirmedTargetYearOnly() {
        confirmedMovement(MovementType.RECEIPT, LocalDate.of(2026, 1, 10), BigDecimal.TEN);
        confirmedMovement(MovementType.RECEIPT, LocalDate.of(2026, 3, 5), BigDecimal.valueOf(5));
        confirmedMovement(MovementType.RECEIPT, LocalDate.of(2026, 5, 1),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4)); // 2라인 → qty 7, 건수 1
        confirmedMovement(MovementType.ISSUE, LocalDate.of(2026, 1, 20), BigDecimal.valueOf(2));
        confirmedMovement(MovementType.ADJUSTMENT, LocalDate.of(2025, 12, 1), BigDecimal.valueOf(8)); // 다른 연도
        movement(MovementType.RECEIPT, LocalDate.of(2026, 2, 1), false, BigDecimal.valueOf(99)); // DRAFT
        authenticated();

        List<MonthlyMovementByTypeResponse> rows = service.getMonthlyMovements(2026);
        assertThat(rows).extracting(MonthlyMovementByTypeResponse::movementType)
                .containsExactly(MovementType.values()); // 모든 유형 0채움 보존
        rows.forEach(r -> assertThat(r.months()).hasSize(12));

        List<MonthlyMovementResponse> receipt = series(rows, MovementType.RECEIPT);
        assertMonth(receipt, 1, 1L, BigDecimal.TEN);
        assertMonth(receipt, 3, 1L, BigDecimal.valueOf(5));
        assertMonth(receipt, 5, 1L, BigDecimal.valueOf(7)); // 2라인 합산, 건수 1(COUNT DISTINCT 전표)
        assertMonth(receipt, 2, 0L, BigDecimal.ZERO);       // DRAFT 제외 → 0

        List<MonthlyMovementResponse> issue = series(rows, MovementType.ISSUE);
        assertMonth(issue, 1, 1L, BigDecimal.valueOf(2));

        // ADJUSTMENT는 2025 전표라 2026 시리즈 전 월 0
        List<MonthlyMovementResponse> adj = series(rows, MovementType.ADJUSTMENT);
        assertThat(adj.stream().mapToLong(MonthlyMovementResponse::count).sum()).isEqualTo(0L);
    }

    // AC-13 — 저재고 경계: Σqty ≤ reorderPoint(경계 포함), 활성만, 카테고리/현재고 동반
    @Test
    void lowStock_includesBoundaryAndZeroStock_excludesAboveAndInactive() {
        Item below = item("LOW-BELOW", catLow, BigDecimal.TEN, true);
        Warehouse whLow = warehouseRepository.save(Warehouse.of("WH-LOW", "저창고", null));
        Location locLow = locationRepository.save(Location.of(whLow, "LOC-LOW", "위치", null, LocationType.BIN));
        stock(below, locLow, BigDecimal.valueOf(5), BigDecimal.ONE); // 5 ≤ 10 → 포함

        Item boundary = item("LOW-BOUND", catLow, BigDecimal.TEN, true);
        stock(boundary, locLow, BigDecimal.TEN, BigDecimal.ONE); // 10 ≤ 10 경계 → 포함

        Item above = item("LOW-ABOVE", catLow, BigDecimal.TEN, true);
        stock(above, locLow, BigDecimal.valueOf(50), BigDecimal.ONE); // 50 > 10 → 제외

        item("LOW-ZERO", catLow, BigDecimal.TEN, true); // 재고 없음 → 0 ≤ 10 → 포함

        Item inactive = item("LOW-INACT", catLow, BigDecimal.TEN, false);
        stock(inactive, locLow, BigDecimal.ONE, BigDecimal.ONE); // 비활성 → 제외
        authenticated();

        List<LowStockItemResponse> rows = service.getLowStockItems();
        assertThat(rows).extracting(LowStockItemResponse::sku)
                .contains("LOW-BELOW", "LOW-BOUND", "LOW-ZERO")
                .doesNotContain("LOW-ABOVE", "LOW-INACT");

        LowStockItemResponse boundaryRow = low(rows, "LOW-BOUND");
        assertThat(boundaryRow.currentQty()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(boundaryRow.reorderPoint()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(boundaryRow.categoryName()).isEqualTo("소모품");

        assertThat(low(rows, "LOW-ZERO").currentQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // AC-14 — INVENTORY_READ 없으면 403(FORBIDDEN)
    @Test
    void noInventoryReadPermission_throwsForbidden() {
        authenticate(List.of("inventory:write")); // read 없음
        assertThrows(ErpException.class, () -> service.getActiveItemsByCategory());
        assertThrows(ErpException.class, () -> service.getStockByWarehouse());
        assertThrows(ErpException.class, () -> service.getMovementsByType());
        assertThrows(ErpException.class, () -> service.getMonthlyMovements(2026));
        assertThrows(ErpException.class, () -> service.getLowStockItems());
    }

    private long catCount(List<CategoryItemCountResponse> rows, Long id) {
        return rows.stream().filter(r -> r.categoryId().equals(id)).findFirst().orElseThrow().count();
    }

    private WarehouseStockResponse warehouse(List<WarehouseStockResponse> rows, Long id) {
        return rows.stream().filter(r -> r.warehouseId().equals(id)).findFirst().orElseThrow();
    }

    private long typeCount(List<MovementTypeCountResponse> rows, MovementType type) {
        return rows.stream().filter(r -> r.movementType() == type).findFirst().orElseThrow().count();
    }

    private List<MonthlyMovementResponse> series(List<MonthlyMovementByTypeResponse> rows, MovementType type) {
        return rows.stream().filter(r -> r.movementType() == type).findFirst().orElseThrow().months();
    }

    private void assertMonth(List<MonthlyMovementResponse> months, int month, long count, BigDecimal qty) {
        MonthlyMovementResponse m = months.stream().filter(r -> r.month() == month).findFirst().orElseThrow();
        assertThat(m.count()).isEqualTo(count);
        assertThat(m.totalQty()).isEqualByComparingTo(qty);
    }

    private LowStockItemResponse low(List<LowStockItemResponse> rows, String sku) {
        return rows.stream().filter(r -> r.sku().equals(sku)).findFirst().orElseThrow();
    }
}
