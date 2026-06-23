package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    List<PipelineStage> findAllByOrderByStageOrderAsc();
}
