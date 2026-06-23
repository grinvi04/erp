package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.WarehouseCreateRequest;
import com.erp.inventory.application.dto.WarehouseResponse;
import com.erp.inventory.application.dto.WarehouseUpdateRequest;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.WarehouseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private WarehouseRepository warehouseRepository;
    @InjectMocks private WarehouseService warehouseService;

    @Test
    void findAll_returnsMappedList() {
        given(warehouseRepository.findAll()).willReturn(List.of(Warehouse.of("WH-001", "본창고", "서울")));

        List<WarehouseResponse> result = warehouseService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("WH-001");
    }

    @Test
    void create_newCode_savesAndReturns() {
        Warehouse wh = Warehouse.of("WH-001", "본창고", "서울");
        given(warehouseRepository.existsByCode("WH-001")).willReturn(false);
        given(warehouseRepository.save(any())).willReturn(wh);

        WarehouseResponse result = warehouseService.create(
                new WarehouseCreateRequest("WH-001", "본창고", "서울"));

        assertThat(result.code()).isEqualTo("WH-001");
        assertThat(result.active()).isTrue();
    }

    @Test
    void create_duplicateCode_throwsWarehouseCodeDuplicate() {
        given(warehouseRepository.existsByCode("WH-001")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class,
                () -> warehouseService.create(new WarehouseCreateRequest("WH-001", "본창고", "서울")));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WAREHOUSE_CODE_DUPLICATE);
    }

    @Test
    void update_existingWarehouse_updatesFields() {
        Warehouse wh = Warehouse.of("WH-001", "본창고", "서울");
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(wh));

        WarehouseResponse result = warehouseService.update(1L,
                new WarehouseUpdateRequest("수정창고", "부산"));

        assertThat(result.name()).isEqualTo("수정창고");
        assertThat(result.address()).isEqualTo("부산");
    }

    @Test
    void deactivate_existingWarehouse_setsInactive() {
        Warehouse wh = Warehouse.of("WH-001", "본창고", "서울");
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(wh));

        warehouseService.deactivate(1L);

        assertThat(wh.isActive()).isFalse();
    }

    @Test
    void findById_notFound_throwsWarehouseNotFound() {
        given(warehouseRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> warehouseService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WAREHOUSE_NOT_FOUND);
    }
}
