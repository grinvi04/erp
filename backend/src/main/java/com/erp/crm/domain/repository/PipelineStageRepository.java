package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    List<PipelineStage> findAllByOrderByStageOrderAsc();

    // 무기회 단계도 포함하기 위해 LEFT JOIN. PipelineStage·Opportunity 모두 @TenantId로
    // 스코프되며 한 테넌트의 단계는 동일 테넌트의 기회만 가지므로(공동 테넌트 FK) 교차
    // 테넌트 오염은 없다. 빈 단계가 count=0으로 보존됨은 통합 테스트로 검증됨.
    // owner 스코프는 LEFT JOIN의 ON 조건에 둬서(WHERE 아님) 매칭 기회가 없는 단계도
    // count=0으로 남도록 한다 — 단계 목록은 owner와 무관하게 전부 보여야 하므로.
    @Query("SELECT s.id AS stageId, s.name AS stageName, s.stageOrder AS stageOrder, "
            + "o.currency AS currency, COUNT(o.id) AS count, COALESCE(SUM(o.amount), 0) AS totalAmount, "
            + "SUM(o.baseAmount) AS baseTotal "
            + "FROM PipelineStage s LEFT JOIN Opportunity o ON o.stage = s "
            + "AND (:scoped = false OR o.ownerId IN :ownerIds) "
            + "GROUP BY s.id, s.name, s.stageOrder, o.currency ORDER BY s.stageOrder, o.currency")
    List<PipelineDistributionRow> pipelineDistribution(@Param("scoped") boolean scoped,
                                                       @Param("ownerIds") java.util.Collection<String> ownerIds);
}
