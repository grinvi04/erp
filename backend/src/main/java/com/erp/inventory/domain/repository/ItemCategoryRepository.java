package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.ItemCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    boolean existsByCode(String code);
    List<ItemCategory> findByParentIsNull();
    List<ItemCategory> findByParent_Id(Long parentId);
    boolean existsByParent_Id(Long parentId);
}
