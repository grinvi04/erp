package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Item;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsBySku(String sku);
    boolean existsByCategory_Id(Long categoryId);
    long countByActiveTrue();

    // keyword는 서비스에서 소문자로 정규화되어 전달된다(LOWER(컬럼)과 매칭 → 대소문자 무시).
    @Query("SELECT i FROM Item i WHERE "
            + "(:keyword IS NULL "
            + "OR LOWER(i.sku) LIKE %:keyword% "
            + "OR LOWER(i.name) LIKE %:keyword%) AND "
            + "(:categoryId IS NULL OR i.category.id = :categoryId)")
    Page<Item> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Item i WHERE i.active = true AND "
            + "(SELECT COALESCE(SUM(s.qtyOnHand), 0) FROM Stock s WHERE s.item = i) <= i.reorderPoint")
    long countLowStockItems();

    // 저재고 활성 품목 목록 — Σ현재고 ≤ reorderPoint(경계 포함). 카테고리 null 품목 보존(LEFT JOIN).
    @Query("SELECT i.sku AS sku, i.name AS name, c.name AS categoryName, "
            + "(SELECT COALESCE(SUM(s.qtyOnHand), 0) FROM Stock s WHERE s.item = i) AS currentQty, "
            + "i.reorderPoint AS reorderPoint "
            + "FROM Item i LEFT JOIN i.category c "
            + "WHERE i.active = true AND "
            + "(SELECT COALESCE(SUM(s.qtyOnHand), 0) FROM Stock s WHERE s.item = i) <= i.reorderPoint "
            + "ORDER BY i.sku")
    List<LowStockItemRow> findLowStockItems();
}
