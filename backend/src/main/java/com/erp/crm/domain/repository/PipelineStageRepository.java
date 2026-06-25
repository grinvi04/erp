package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    List<PipelineStage> findAllByOrderByStageOrderAsc();

    // 무기회 단계도 포함하기 위해 LEFT JOIN. PipelineStage·Opportunity 모두 @TenantId로
    // 스코프되며 한 테넌트의 단계는 동일 테넌트의 기회만 가지므로(공동 테넌트 FK) 교차
    // 테넌트 오염은 없다. 빈 단계가 count=0으로 보존됨은 통합 테스트로 검증됨.
    @Query("SELECT s.id AS stageId, s.name AS stageName, s.stageOrder AS stageOrder, "
            + "COUNT(o.id) AS count, COALESCE(SUM(o.amount), 0) AS totalAmount "
            + "FROM PipelineStage s LEFT JOIN Opportunity o ON o.stage = s "
            + "GROUP BY s.id, s.name, s.stageOrder ORDER BY s.stageOrder")
    List<PipelineDistributionRow> pipelineDistribution();
}
