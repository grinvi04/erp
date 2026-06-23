package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.LocationCreateRequest;
import com.erp.inventory.application.dto.LocationResponse;
import com.erp.inventory.application.dto.LocationUpdateRequest;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final WarehouseService warehouseService;

    public List<LocationResponse> findByWarehouse(Long warehouseId) {
        warehouseService.getOrThrow(warehouseId);
        return locationRepository.findByWarehouse_Id(warehouseId).stream()
                .map(LocationResponse::from).toList();
    }

    public List<LocationResponse> findChildren(Long parentId) {
        getOrThrow(parentId);
        return locationRepository.findByParent_Id(parentId).stream()
                .map(LocationResponse::from).toList();
    }

    public LocationResponse findById(Long id) {
        return LocationResponse.from(getOrThrow(id));
    }

    @Transactional
    public LocationResponse create(LocationCreateRequest req) {
        Warehouse warehouse = warehouseService.getOrThrow(req.warehouseId());
        if (locationRepository.existsByWarehouse_IdAndCode(req.warehouseId(), req.code().toUpperCase())) {
            throw new ErpException(ErrorCode.LOCATION_CODE_DUPLICATE);
        }
        Location parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
        return LocationResponse.from(locationRepository.save(
                Location.of(warehouse, req.code(), req.name(), parent, req.locationType())));
    }

    @Transactional
    public LocationResponse update(Long id, LocationUpdateRequest req) {
        Location loc = getOrThrow(id);
        Location parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
        loc.update(req.name(), parent, req.locationType());
        return LocationResponse.from(loc);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).deactivate();
    }

    public Location getOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.LOCATION_NOT_FOUND));
    }
}
