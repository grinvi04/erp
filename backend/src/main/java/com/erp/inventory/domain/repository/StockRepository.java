package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.Stock;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByItemAndLocationAndLotNoAndSerialNo(
            Item item, Location location, String lotNo, String serialNo);

    Page<Stock> findByItem_Id(Long itemId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.location.warehouse.id = :warehouseId")
    Page<Stock> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);
}
