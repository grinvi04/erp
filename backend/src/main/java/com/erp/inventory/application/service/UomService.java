package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.inventory.application.dto.UomCreateRequest;
import com.erp.inventory.application.dto.UomResponse;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UomService {

    private final UnitOfMeasureRepository uomRepository;
    private final PermissionChecker permissionChecker;

    public List<UomResponse> findAll() {
        permissionChecker.require(Permission.INVENTORY_READ);
        return uomRepository.findAll().stream().map(UomResponse::from).toList();
    }

    public UomResponse findById(Long id) {
        permissionChecker.require(Permission.INVENTORY_READ);
        return UomResponse.from(getOrThrow(id));
    }

    @Transactional
    public UomResponse create(UomCreateRequest req) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        if (uomRepository.existsByCode(req.code().toUpperCase())) {
            throw new ErpException(ErrorCode.UOM_CODE_DUPLICATE);
        }
        return UomResponse.from(uomRepository.save(UnitOfMeasure.of(req.code(), req.name())));
    }

    @Transactional
    public UomResponse update(Long id, UomCreateRequest req) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        UnitOfMeasure uom = getOrThrow(id);
        uom.update(req.name());
        return UomResponse.from(uom);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.require(Permission.INVENTORY_WRITE);
        UnitOfMeasure uom = getOrThrow(id);
        uom.softDelete();
    }

    public UnitOfMeasure getEntityOrThrow(Long id) {
        return uomRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.UOM_NOT_FOUND));
    }

    private UnitOfMeasure getOrThrow(Long id) {
        return getEntityOrThrow(id);
    }
}
