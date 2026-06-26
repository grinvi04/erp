package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.PipelineStageCreateRequest;
import com.erp.crm.application.dto.PipelineStageResponse;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.PipelineStageRepository;
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
class PipelineStageServiceTest {

    @Mock private PipelineStageRepository stageRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private PipelineStageService stageService;

    private PipelineStage buildStage() {
        return PipelineStage.of("탐색", 1, 20, false, false);
    }

    @Test
    void findAll_returnsOrderedStages() {
        given(stageRepository.findAllByOrderByStageOrderAsc())
                .willReturn(List.of(buildStage()));

        List<PipelineStageResponse> result = stageService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("탐색");
        assertThat(result.get(0).stageOrder()).isEqualTo(1);
    }

    @Test
    void create_validRequest_savesAndReturns() {
        PipelineStage stage = buildStage();
        given(stageRepository.save(any())).willReturn(stage);

        PipelineStageCreateRequest req = new PipelineStageCreateRequest("탐색", 1, 20, false, false);

        PipelineStageResponse result = stageService.create(req);

        assertThat(result.name()).isEqualTo("탐색");
        assertThat(result.probability()).isEqualTo(20);
    }

    @Test
    void findById_notFound_throwsPipelineStageNotFound() {
        given(stageRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> stageService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_STAGE_NOT_FOUND);
    }

    @Test
    void update_existingStage_appliesChanges() {
        PipelineStage stage = buildStage();
        given(stageRepository.findById(1L)).willReturn(Optional.of(stage));

        PipelineStageCreateRequest req = new PipelineStageCreateRequest("제안", 2, 60, false, false);

        PipelineStageResponse result = stageService.update(1L, req);

        assertThat(result.name()).isEqualTo("제안");
        assertThat(result.stageOrder()).isEqualTo(2);
        assertThat(result.probability()).isEqualTo(60);
    }

    @Test
    void update_notFound_throwsPipelineStageNotFound() {
        given(stageRepository.findById(99L)).willReturn(Optional.empty());

        PipelineStageCreateRequest req = new PipelineStageCreateRequest("제안", 2, 60, false, false);

        ErpException ex = assertThrows(ErpException.class, () -> stageService.update(99L, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_STAGE_NOT_FOUND);
    }

    @Test
    void getOrThrow_notFound_throwsPipelineStageNotFound() {
        given(stageRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> stageService.getOrThrow(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_STAGE_NOT_FOUND);
    }
}
