package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.PositionCreateRequest;
import com.erp.hr.application.dto.PositionResponse;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PositionService positionService;

    @Test
    void create_newCode_returnsCreatedPosition() {
        given(positionRepository.existsByCode("MGR")).willReturn(false);
        Position position = Position.of("MGR", "과장", 3);
        given(positionRepository.save(any())).willReturn(position);

        PositionResponse result = positionService.create(
            new PositionCreateRequest("MGR", "과장", 3));

        assertThat(result.code()).isEqualTo("MGR");
        assertThat(result.levelOrder()).isEqualTo(3);
    }

    @Test
    void create_duplicateCode_throwsDuplicateCode() {
        given(positionRepository.existsByCode("MGR")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            positionService.create(new PositionCreateRequest("MGR", "과장", 3)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void delete_positionInUse_throwsPositionInUse() {
        Position position = Position.of("MGR", "과장", 3);
        given(positionRepository.findById(1L)).willReturn(Optional.of(position));
        given(employeeRepository.existsByPositionId(1L)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () -> positionService.delete(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POSITION_IN_USE);
    }

    @Test
    void delete_notInUse_callsSoftDelete() {
        Position position = Position.of("MGR", "과장", 3);
        given(positionRepository.findById(1L)).willReturn(Optional.of(position));
        given(employeeRepository.existsByPositionId(1L)).willReturn(false);

        positionService.delete(1L);

        assertThat(position.isDeleted()).isTrue();
    }
}
