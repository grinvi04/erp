package com.erp.crm.application.dto;

import com.erp.crm.domain.model.PipelineStage;

public record PipelineStageResponse(
        Long id,
        String name,
        int stageOrder,
        int probability,
        boolean isClosedWon,
        boolean isClosedLost,
        Long version
) {
    public static PipelineStageResponse from(PipelineStage s) {
        return new PipelineStageResponse(s.getId(), s.getName(), s.getStageOrder(),
                s.getProbability(), s.isClosedWon(), s.isClosedLost(), s.getVersion());
    }
}
