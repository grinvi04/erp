package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "fiscal_period", schema = "finance")
public class FiscalPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fiscal_period_seq")
    @SequenceGenerator(name = "fiscal_period_seq", sequenceName = "finance.fiscal_period_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FiscalPeriodStatus status;

    protected FiscalPeriod() {}

    public static FiscalPeriod of(FiscalYear fiscalYear, int periodNumber,
                                  LocalDate startDate, LocalDate endDate) {
        FiscalPeriod fp = new FiscalPeriod();
        fp.fiscalYear = fiscalYear;
        fp.periodNumber = periodNumber;
        fp.startDate = startDate;
        fp.endDate = endDate;
        fp.status = FiscalPeriodStatus.OPEN;
        return fp;
    }

    public void close() {
        if (this.status != FiscalPeriodStatus.OPEN) {
            throw new ErpException(ErrorCode.FISCAL_PERIOD_ALREADY_CLOSED);
        }
        this.status = FiscalPeriodStatus.CLOSED;
    }

    public void lock() {
        this.status = FiscalPeriodStatus.LOCKED;
    }

    public boolean isOpen() { return status == FiscalPeriodStatus.OPEN; }

    public Long getId() { return id; }
    public FiscalYear getFiscalYear() { return fiscalYear; }
    public int getPeriodNumber() { return periodNumber; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public FiscalPeriodStatus getStatus() { return status; }
}
