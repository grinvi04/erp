package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.LocationCreateRequest;
import com.erp.inventory.application.dto.LocationResponse;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.LocationRepository;
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
class LocationServiceTest {

    @Mock private LocationRepository locationRepository;
    @Mock private WarehouseService warehouseService;
    @InjectMocks private LocationService locationService;

    private Warehouse buildWarehouse() {
        return Warehouse.of("WH-001", "본창고", "서울");
    }

    @Test
    void findByWarehouse_returnsList() {
        Warehouse wh = buildWarehouse();
        Location loc = Location.of(wh, "A-01", "구역A", null, LocationType.ZONE);
        given(warehouseService.getOrThrow(1L)).willReturn(wh);
        given(locationRepository.findByWarehouse_Id(1L)).willReturn(List.of(loc));

        List<LocationResponse> result = locationService.findByWarehouse(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("A-01");
    }

    @Test
    void create_newCode_savesAndReturns() {
        Warehouse wh = buildWarehouse();
        Location loc = Location.of(wh, "A-01", "구역A", null, LocationType.ZONE);
        given(warehouseService.getOrThrow(1L)).willReturn(wh);
        given(locationRepository.existsByWarehouse_IdAndCode(1L, "A-01")).willReturn(false);
        given(locationRepository.save(any())).willReturn(loc);

        LocationResponse result = locationService.create(
                new LocationCreateRequest(1L, "A-01", "구역A", null, LocationType.ZONE));

        assertThat(result.code()).isEqualTo("A-01");
        assertThat(result.locationType()).isEqualTo(LocationType.ZONE);
    }

    @Test
    void create_duplicateCode_throwsLocationCodeDuplicate() {
        Warehouse wh = buildWarehouse();
        given(warehouseService.getOrThrow(1L)).willReturn(wh);
        given(locationRepository.existsByWarehouse_IdAndCode(1L, "A-01")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class,
                () -> locationService.create(
                        new LocationCreateRequest(1L, "A-01", "구역A", null, LocationType.ZONE)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOCATION_CODE_DUPLICATE);
    }

    @Test
    void findById_notFound_throwsLocationNotFound() {
        given(locationRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> locationService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOCATION_NOT_FOUND);
    }

    @Test
    void deactivate_existingLocation_setsInactive() {
        Warehouse wh = buildWarehouse();
        Location loc = Location.of(wh, "A-01", "구역A", null, LocationType.ZONE);
        given(locationRepository.findById(1L)).willReturn(Optional.of(loc));

        locationService.deactivate(1L);

        assertThat(loc.isActive()).isFalse();
    }
}
