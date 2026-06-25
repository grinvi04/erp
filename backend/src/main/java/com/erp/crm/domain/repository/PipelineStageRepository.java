package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    List<PipelineStage> findAllByOrderByStageOrderAsc();

    @Query("SELECT s.id AS stageId, s.name AS stageName, s.stageOrder AS stageOrder, "
            + "COUNT(o.id) AS count, COALESCE(SUM(o.amount), 0) AS totalAmount "
            + "FROM PipelineStage s LEFT JOIN Opportunity o ON o.stage = s "
            + "GROUP BY s.id, s.name, s.stageOrder ORDER BY s.stageOrder")
    List<PipelineDistributionRow> pipelineDistribution();
}
