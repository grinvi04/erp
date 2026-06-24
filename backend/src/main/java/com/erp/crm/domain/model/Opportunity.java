package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "opportunity", schema = "crm")
public class Opportunity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_opportunity_seq")
    @SequenceGenerator(name = "crm_opportunity_seq", sequenceName = "crm.opportunity_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private PipelineStage stage;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "probability", nullable = false)
    private int probability;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected Opportunity() {}

    public static Opportunity of(Account account, String name, PipelineStage stage,
                                 BigDecimal amount, String currency, LocalDate closeDate,
                                 int probability, String ownerId, String source, String description) {
        Opportunity o = new Opportunity();
        o.account = account;
        o.name = name;
        o.stage = stage;
        o.amount = amount;
        o.currency = currency != null ? currency : "KRW";
        o.closeDate = closeDate;
        o.probability = probability;
        o.ownerId = ownerId;
        o.source = source;
        o.description = description;
        return o;
    }

    public void update(String name, PipelineStage stage, BigDecimal amount, String currency,
                       LocalDate closeDate, int probability, String ownerId,
                       String source, String description) {
        this.name = name;
        this.stage = stage;
        this.amount = amount;
        this.currency = currency != null ? currency : "KRW";
        this.closeDate = closeDate;
        this.probability = probability;
        this.ownerId = ownerId;
        this.source = source;
        this.description = description;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getName() { return name; }
    public PipelineStage getStage() { return stage; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getCloseDate() { return closeDate; }
    public int getProbability() { return probability; }
    public String getOwnerId() { return ownerId; }
    public String getSource() { return source; }
    public String getDescription() { return description; }
}
