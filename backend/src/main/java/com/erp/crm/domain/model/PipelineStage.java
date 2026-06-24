package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_stage", schema = "crm")
public class PipelineStage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pipeline_stage_seq")
    @SequenceGenerator(name = "pipeline_stage_seq", sequenceName = "crm.pipeline_stage_id_seq", allocationSize = 20)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "stage_order", nullable = false)
    private int stageOrder;

    @Column(name = "probability", nullable = false)
    private int probability;

    @Column(name = "is_closed_won", nullable = false)
    private boolean isClosedWon;

    @Column(name = "is_closed_lost", nullable = false)
    private boolean isClosedLost;

    protected PipelineStage() {}

    public static PipelineStage of(String name, int stageOrder, int probability,
                                   boolean isClosedWon, boolean isClosedLost) {
        PipelineStage s = new PipelineStage();
        s.name = name;
        s.stageOrder = stageOrder;
        s.probability = probability;
        s.isClosedWon = isClosedWon;
        s.isClosedLost = isClosedLost;
        return s;
    }

    public void update(String name, int stageOrder, int probability,
                       boolean isClosedWon, boolean isClosedLost) {
        this.name = name;
        this.stageOrder = stageOrder;
        this.probability = probability;
        this.isClosedWon = isClosedWon;
        this.isClosedLost = isClosedLost;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getStageOrder() { return stageOrder; }
    public int getProbability() { return probability; }
    public boolean isClosedWon() { return isClosedWon; }
    public boolean isClosedLost() { return isClosedLost; }
}
