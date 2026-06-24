package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.WarehouseCreateRequest;
import com.erp.inventory.application.dto.WarehouseResponse;
import com.erp.inventory.application.dto.WarehouseUpdateRequest;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.WarehouseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public List<WarehouseResponse> findAll() {
        return warehouseRepository.findAll().stream().map(WarehouseResponse::from).toList();
    }

    public WarehouseResponse findById(Long id) {
        return WarehouseResponse.from(getOrThrow(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest req) {
        if (warehouseRepository.existsByCode(req.code().toUpperCase())) {
            throw new ErpException(ErrorCode.WAREHOUSE_CODE_DUPLICATE);
        }
        return WarehouseResponse.from(
                warehouseRepository.save(Warehouse.of(req.code(), req.name(), req.address())));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseUpdateRequest req) {
        Warehouse w = getOrThrow(id);
        w.update(req.name(), req.address());
        return WarehouseResponse.from(w);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).deactivate();
    }

    public Warehouse getOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }
}
