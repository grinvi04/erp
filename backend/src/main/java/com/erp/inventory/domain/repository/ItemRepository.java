package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsBySku(String sku);
    boolean existsByCategory_Id(Long categoryId);
    Page<Item> findByCategory_Id(Long categoryId, Pageable pageable);
}
