package com.erp.inventory.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.inventory.application.dto.CategoryItemCountResponse;
import com.erp.inventory.application.dto.LowStockItemResponse;
import com.erp.inventory.application.dto.MonthlyMovementByTypeResponse;
import com.erp.inventory.application.dto.MonthlyMovementResponse;
import com.erp.inventory.application.dto.MovementTypeCountResponse;
import com.erp.inventory.application.dto.WarehouseStockResponse;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.MonthlyMovementRow;
import com.erp.inventory.domain.repository.MovementLineRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import com.erp.inventory.domain.repository.MovementTypeCountRow;
import com.erp.inventory.domain.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 분석 집계 — Hr/FinanceAnalytics 청사진 복제. INVENTORY_READ 권한만 검사(데이터 스코프 없음).
 * 이동유형 분포는 enum 전체를 0채움, 카테고리/창고는 빈 그룹을 보존, 월별은 1~12월 0채움.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private static final int MONTHS_IN_YEAR = 12;

    private final ItemCategoryRepository itemCategoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementRepository movementRepository;
    private final MovementLineRepository movementLineRepository;
    private final ItemRepository itemRepository;
    private final PermissionChecker permissionChecker;

    public List<CategoryItemCountResponse> getActiveItemsByCategory() {
        permissionChecker.require(Permission.INVENTORY_READ);
        return itemCategoryRepository.activeItemCountByCategory().stream()
                .map(r -> new CategoryItemCountResponse(r.getCategoryId(), r.getCategoryName(), r.getCount()))
                .collect(Collectors.toList());
    }

    public List<WarehouseStockResponse> getStockByWarehouse() {
        permissionChecker.require(Permission.INVENTORY_READ);
        return warehouseRepository.stockByWarehouse().stream()
                .map(r -> new WarehouseStockResponse(
                        r.getWarehouseId(), r.getWarehouseName(), r.getTotalQty(), r.getTotalValue()))
                .collect(Collectors.toList());
    }

    public List<MovementTypeCountResponse> getMovementsByType() {
        permissionChecker.require(Permission.INVENTORY_READ);
        Map<MovementType, Long> countMap = movementRepository.confirmedCountByType().stream()
                .collect(Collectors.toMap(MovementTypeCountRow::getMovementType, MovementTypeCountRow::getCount));
        return Arrays.stream(MovementType.values())
                .map(type -> new MovementTypeCountResponse(type, countMap.getOrDefault(type, 0L)))
                .collect(Collectors.toList());
    }

    public List<MonthlyMovementByTypeResponse> getMonthlyMovements(Integer year) {
        permissionChecker.require(Permission.INVENTORY_READ);
        int targetYear = year != null ? year : Year.now().getValue();

        // 이동유형별로 월별 행을 모은다. enum 순서로 시리즈를 만들어 빈 유형도 12개월 0채움으로 보존.
        Map<MovementType, Map<Integer, MonthlyMovementRow>> byType = new LinkedHashMap<>();
        for (MovementType type : MovementType.values()) {
            byType.put(type, new LinkedHashMap<>());
        }
        for (MonthlyMovementRow row : movementLineRepository.monthlyMovementsByType(targetYear)) {
            byType.get(row.getMovementType()).put(row.getMonth(), row);
        }

        List<MonthlyMovementByTypeResponse> result = new ArrayList<>(byType.size());
        for (Map.Entry<MovementType, Map<Integer, MonthlyMovementRow>> entry : byType.entrySet()) {
            Map<Integer, MonthlyMovementRow> rowMap = entry.getValue();
            List<MonthlyMovementResponse> months = new ArrayList<>(MONTHS_IN_YEAR);
            for (int month = 1; month <= MONTHS_IN_YEAR; month++) {
                MonthlyMovementRow row = rowMap.get(month);
                if (row != null) {
                    months.add(new MonthlyMovementResponse(month, row.getCount(), row.getTotalQty()));
                } else {
                    months.add(new MonthlyMovementResponse(month, 0L, BigDecimal.ZERO));
                }
            }
            result.add(new MonthlyMovementByTypeResponse(entry.getKey(), months));
        }
        return result;
    }

    public List<LowStockItemResponse> getLowStockItems() {
        permissionChecker.require(Permission.INVENTORY_READ);
        return itemRepository.findLowStockItems().stream()
                .map(r -> new LowStockItemResponse(
                        r.getSku(), r.getName(), r.getCategoryName(), r.getCurrentQty(), r.getReorderPoint()))
                .collect(Collectors.toList());
    }
}
