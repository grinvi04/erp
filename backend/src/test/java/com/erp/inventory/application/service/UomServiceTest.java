package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.UomCreateRequest;
import com.erp.inventory.application.dto.UomResponse;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
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
class UomServiceTest {

    @Mock private UnitOfMeasureRepository uomRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private UomService uomService;

    @Test
    void findAll_returnsMappedList() {
        UnitOfMeasure uom = UnitOfMeasure.of("EA", "개");
        given(uomRepository.findAll()).willReturn(List.of(uom));

        List<UomResponse> result = uomService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("EA");
    }

    @Test
    void create_newCode_savesAndReturns() {
        UnitOfMeasure uom = UnitOfMeasure.of("EA", "개");
        given(uomRepository.existsByCode("EA")).willReturn(false);
        given(uomRepository.save(any())).willReturn(uom);

        UomResponse result = uomService.create(new UomCreateRequest("EA", "개"));

        assertThat(result.code()).isEqualTo("EA");
    }

    @Test
    void create_duplicateCode_throwsUomCodeDuplicate() {
        given(uomRepository.existsByCode("EA")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class,
                () -> uomService.create(new UomCreateRequest("EA", "개")));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UOM_CODE_DUPLICATE);
    }

    @Test
    void findById_notFound_throwsUomNotFound() {
        given(uomRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> uomService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UOM_NOT_FOUND);
    }

    @Test
    void delete_existingUom_softDeletes() {
        UnitOfMeasure uom = UnitOfMeasure.of("EA", "개");
        given(uomRepository.findById(1L)).willReturn(Optional.of(uom));

        uomService.delete(1L);

        assertThat(uom.getDeletedAt()).isNotNull();
    }
}
