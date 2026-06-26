package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Warehouse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByCode(String code);

    // 활성 창고별 재고 수량(Σqty)·가치(Σqty×unitCost). 재고 없는 창고도 LEFT JOIN으로 0 보존.
    // Warehouse→Location→Stock는 선형 조인이라 중복 합산 없음. 단일 기준통화(₩)로 BigDecimal 합산.
    @Query("SELECT w.id AS warehouseId, w.name AS warehouseName, "
            + "COALESCE(SUM(s.qtyOnHand), 0) AS totalQty, "
            + "COALESCE(SUM(s.qtyOnHand * s.unitCost), 0) AS totalValue "
            + "FROM Warehouse w "
            + "LEFT JOIN Location l ON l.warehouse = w "
            + "LEFT JOIN Stock s ON s.location = l "
            + "WHERE w.active = true "
            + "GROUP BY w.id, w.name ORDER BY w.name, w.id")
    List<WarehouseStockRow> stockByWarehouse();
}
