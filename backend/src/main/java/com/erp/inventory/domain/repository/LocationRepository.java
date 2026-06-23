package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.Location;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByWarehouse_IdAndCode(Long warehouseId, String code);
    List<Location> findByWarehouse_Id(Long warehouseId);
    List<Location> findByParent_Id(Long parentId);
}
