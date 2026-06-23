package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 직급/호봉 — 급여 밴드의 기준.
 */
@Entity
@Table(name = "job_grade", schema = "hr")
public class JobGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_grade_seq")
    @SequenceGenerator(name = "job_grade_seq", sequenceName = "hr.job_grade_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "grade_order", nullable = false)
    private int gradeOrder;

    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;

    protected JobGrade() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getGradeOrder() { return gradeOrder; }
    public BigDecimal getMinSalary() { return minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
}
