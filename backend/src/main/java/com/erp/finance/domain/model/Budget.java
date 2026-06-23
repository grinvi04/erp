package com.erp.finance.domain.model;

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

@Entity
@Table(name = "budget", schema = "finance")
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_seq")
    @SequenceGenerator(name = "budget_seq", sequenceName = "finance.budget_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "budget_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "actual_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal actualAmount;

    protected Budget() {}

    public static Budget of(FiscalYear fiscalYear, Account account, Long departmentId,
                             BigDecimal budgetAmount) {
        Budget b = new Budget();
        b.fiscalYear = fiscalYear;
        b.account = account;
        b.departmentId = departmentId;
        b.budgetAmount = budgetAmount;
        b.actualAmount = BigDecimal.ZERO;
        return b;
    }

    public void updateBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public void incrementActual(BigDecimal amount) {
        this.actualAmount = this.actualAmount.add(amount);
    }

    public void decrementActual(BigDecimal amount) {
        this.actualAmount = this.actualAmount.subtract(amount);
        if (this.actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.actualAmount = BigDecimal.ZERO;
        }
    }

    public BigDecimal getRemainingBudget() {
        return budgetAmount.subtract(actualAmount);
    }

    public boolean isOverBudget() {
        return actualAmount.compareTo(budgetAmount) > 0;
    }

    public Long getId() { return id; }
    public FiscalYear getFiscalYear() { return fiscalYear; }
    public Account getAccount() { return account; }
    public Long getDepartmentId() { return departmentId; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
}
