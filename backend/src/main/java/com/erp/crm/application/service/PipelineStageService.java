package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.PipelineStageCreateRequest;
import com.erp.crm.application.dto.PipelineStageResponse;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PipelineStageService {

    private final PipelineStageRepository stageRepository;

    public List<PipelineStageResponse> findAll() {
        return stageRepository.findAllByOrderByStageOrderAsc().stream()
                .map(PipelineStageResponse::from).toList();
    }

    public PipelineStageResponse findById(Long id) {
        return PipelineStageResponse.from(getOrThrow(id));
    }

    @Transactional
    public PipelineStageResponse create(PipelineStageCreateRequest req) {
        PipelineStage stage = PipelineStage.of(req.name(), req.stageOrder(), req.probability(),
                req.isClosedWon(), req.isClosedLost());
        return PipelineStageResponse.from(stageRepository.save(stage));
    }

    @Transactional
    public PipelineStageResponse update(Long id, PipelineStageCreateRequest req) {
        PipelineStage stage = getOrThrow(id);
        stage.update(req.name(), req.stageOrder(), req.probability(),
                req.isClosedWon(), req.isClosedLost());
        return PipelineStageResponse.from(stage);
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id).softDelete();
    }

    public PipelineStage getOrThrow(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.PIPELINE_STAGE_NOT_FOUND));
    }
}
