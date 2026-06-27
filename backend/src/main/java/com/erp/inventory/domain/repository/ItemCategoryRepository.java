package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.ItemCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
  boolean existsByCode(String code);

  List<ItemCategory> findByParentIsNull();

  List<ItemCategory> findByParent_Id(Long parentId);

  boolean existsByParent_Id(Long parentId);

  // 카테고리별 활성 품목 수 — 품목 0인 카테고리도 LEFT JOIN으로 보존(count=0).
  @Query(
      "SELECT c.id AS categoryId, c.name AS categoryName, COUNT(i.id) AS count "
          + "FROM ItemCategory c LEFT JOIN Item i ON i.category = c AND i.active = true "
          + "GROUP BY c.id, c.name ORDER BY c.name, c.id")
  List<CategoryItemCountRow> activeItemCountByCategory();
}
