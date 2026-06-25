package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsBySku(String sku);
    boolean existsByCategory_Id(Long categoryId);
    Page<Item> findByCategory_Id(Long categoryId, Pageable pageable);
    long countByActiveTrue();

    @Query("SELECT COUNT(i) FROM Item i WHERE i.active = true AND "
            + "(SELECT COALESCE(SUM(s.qtyOnHand), 0) FROM Stock s WHERE s.item = i) <= i.reorderPoint")
    long countLowStockItems();
}
