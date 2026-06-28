package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "fiscal_year", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class FiscalYear extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fiscal_year_seq")
  @SequenceGenerator(
      name = "fiscal_year_seq",
      sequenceName = "finance.fiscal_year_id_seq",
      allocationSize = 10)
  private Long id;

  @Column(name = "year", nullable = false)
  private int year;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private FiscalYearStatus status;

  protected FiscalYear() {}

  public static FiscalYear of(int year, LocalDate startDate, LocalDate endDate) {
    FiscalYear fy = new FiscalYear();
    fy.year = year;
    fy.startDate = startDate;
    fy.endDate = endDate;
    fy.status = FiscalYearStatus.OPEN;
    return fy;
  }

  public void close() {
    if (this.status == FiscalYearStatus.CLOSED) {
      throw new ErpException(ErrorCode.FISCAL_YEAR_ALREADY_CLOSED);
    }
    this.status = FiscalYearStatus.CLOSED;
  }

  public boolean isOpen() {
    return status == FiscalYearStatus.OPEN;
  }

  public Long getId() {
    return id;
  }

  public int getYear() {
    return year;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public FiscalYearStatus getStatus() {
    return status;
  }
}
