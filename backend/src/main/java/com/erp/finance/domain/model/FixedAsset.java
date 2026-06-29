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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

/**
 * 고정자산 — 취득원가·잔존가치·내용연수·상각방법으로 월별 감가상각을 산정한다. 누계상각액을 보유하며 장부가액=취득원가−누계상각액. 잔존가치 미만으로는 상각하지 않는다(과대상각
 * 금지). 회계상 상각만(세무조정 범위 밖).
 */
@Entity
@Table(name = "fixed_asset", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class FixedAsset extends BaseEntity {

  private static final int MONEY_SCALE = 2;
  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fixed_asset_seq")
  @SequenceGenerator(
      name = "fixed_asset_seq",
      sequenceName = "finance.fixed_asset_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "acquisition_date", nullable = false)
  private LocalDate acquisitionDate;

  @Column(name = "acquisition_cost", nullable = false, precision = 20, scale = 2)
  private BigDecimal acquisitionCost;

  @Column(name = "residual_value", nullable = false, precision = 20, scale = 2)
  private BigDecimal residualValue;

  @Column(name = "useful_life_months", nullable = false)
  private int usefulLifeMonths;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false, length = 20)
  private DepreciationMethod method;

  // 정률법 연상각률(예: 0.45) — 정액법이면 미사용(null).
  @Column(name = "declining_annual_rate", precision = 6, scale = 4)
  private BigDecimal decliningAnnualRate;

  // 유형자산 계정(대변에서 처분 시 제거할 자산 계정).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_account_id", nullable = false)
  private Account assetAccount;

  @Column(name = "accumulated_depreciation", nullable = false, precision = 20, scale = 2)
  private BigDecimal accumulatedDepreciation;

  // 손상차손누계액 — 손상 인식으로 장부가액을 감액한 누계(손상차손누계액 계정 대변과 대응).
  @Column(name = "accumulated_impairment", nullable = false, precision = 20, scale = 2)
  private BigDecimal accumulatedImpairment;

  // 손상 후 정액 월상각액(잔여내용연수 재배분 결과) — 손상 시점에 1회 산정해 고정. 정률·비손상이면 null(기존 상수 공식 사용).
  @Column(name = "straight_line_monthly_override", precision = 20, scale = 2)
  private BigDecimal straightLineMonthlyOverride;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private FixedAssetStatus status;

  protected FixedAsset() {}

  public static FixedAsset register(
      String code,
      String name,
      LocalDate acquisitionDate,
      BigDecimal acquisitionCost,
      BigDecimal residualValue,
      int usefulLifeMonths,
      DepreciationMethod method,
      BigDecimal decliningAnnualRate,
      Account assetAccount) {
    if (acquisitionCost == null || acquisitionCost.signum() <= 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "취득원가는 0보다 커야 합니다");
    }
    if (residualValue == null
        || residualValue.signum() < 0
        || residualValue.compareTo(acquisitionCost) > 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "잔존가치는 0 이상 취득원가 이하여야 합니다");
    }
    if (usefulLifeMonths <= 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "내용연수(월)는 0보다 커야 합니다");
    }
    if (method == DepreciationMethod.DECLINING_BALANCE
        && (decliningAnnualRate == null || decliningAnnualRate.signum() <= 0)) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "정률법은 연상각률이 필요합니다");
    }
    FixedAsset a = new FixedAsset();
    a.code = code;
    a.name = name;
    a.acquisitionDate = acquisitionDate;
    a.acquisitionCost = acquisitionCost.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    a.residualValue = residualValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    a.usefulLifeMonths = usefulLifeMonths;
    a.method = method;
    a.decliningAnnualRate = decliningAnnualRate;
    a.assetAccount = assetAccount;
    a.accumulatedDepreciation = BigDecimal.ZERO.setScale(MONEY_SCALE);
    a.accumulatedImpairment = BigDecimal.ZERO.setScale(MONEY_SCALE);
    a.status = FixedAssetStatus.ACTIVE;
    return a;
  }

  /** 장부가액 = 취득원가 − 감가상각누계액 − 손상차손누계액. */
  public BigDecimal bookValue() {
    return acquisitionCost.subtract(accumulatedDepreciation).subtract(accumulatedImpairment);
  }

  /** 이번 달 상각액 — 방법별 산정 후 잔존가치 하한·과대상각 방지로 보정. 잔존가치에 도달했거나 처분 자산이면 0. */
  public BigDecimal monthlyDepreciation() {
    if (status != FixedAssetStatus.ACTIVE) {
      return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }
    BigDecimal depreciableRemaining = bookValue().subtract(residualValue);
    if (depreciableRemaining.signum() <= 0) {
      return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }
    BigDecimal raw;
    if (method == DepreciationMethod.STRAIGHT_LINE) {
      // 손상 후에는 잔여내용연수로 재배분한 월상각액(override)을, 아니면 기존 상수 공식을 쓴다.
      raw =
          straightLineMonthlyOverride != null
              ? straightLineMonthlyOverride
              : acquisitionCost
                  .subtract(residualValue)
                  .divide(BigDecimal.valueOf(usefulLifeMonths), MONEY_SCALE, RoundingMode.DOWN);
    } else {
      raw =
          bookValue()
              .multiply(decliningAnnualRate)
              .divide(MONTHS_PER_YEAR, MONEY_SCALE, RoundingMode.DOWN);
    }
    // 마지막 기간은 잔존가치까지만(과대상각·누적오차 보정).
    return raw.min(depreciableRemaining);
  }

  /** 상각액 반영 — 누계상각액 증가. */
  public void applyDepreciation(BigDecimal amount) {
    this.accumulatedDepreciation = this.accumulatedDepreciation.add(amount);
  }

  /**
   * 손상차손 반영 — 손상차손누계액을 증가시켜 장부가액을 회수가능액까지 감액한다. 정액법이면 손상 후 (장부가액−잔존가치)를 잔여내용연수로 재배분한 월상각액을 1회 산정해
   * 고정한다(이후 monthlyDepreciation이 이 값을 사용). 정률법은 월초 장부가액 기반이라 별도 설정 없이 자동 반영된다.
   */
  public void applyImpairment(BigDecimal loss, int remainingMonths) {
    this.accumulatedImpairment = this.accumulatedImpairment.add(loss);
    if (method == DepreciationMethod.STRAIGHT_LINE) {
      int months = Math.max(remainingMonths, 1);
      this.straightLineMonthlyOverride =
          bookValue()
              .subtract(residualValue)
              .max(BigDecimal.ZERO)
              .divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.DOWN);
    }
  }

  public void dispose() {
    this.status = FixedAssetStatus.DISPOSED;
  }

  public boolean isActive() {
    return status == FixedAssetStatus.ACTIVE;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public LocalDate getAcquisitionDate() {
    return acquisitionDate;
  }

  public BigDecimal getAcquisitionCost() {
    return acquisitionCost;
  }

  public BigDecimal getResidualValue() {
    return residualValue;
  }

  public int getUsefulLifeMonths() {
    return usefulLifeMonths;
  }

  public DepreciationMethod getMethod() {
    return method;
  }

  public BigDecimal getDecliningAnnualRate() {
    return decliningAnnualRate;
  }

  public Account getAssetAccount() {
    return assetAccount;
  }

  public BigDecimal getAccumulatedDepreciation() {
    return accumulatedDepreciation;
  }

  public BigDecimal getAccumulatedImpairment() {
    return accumulatedImpairment;
  }

  public BigDecimal getStraightLineMonthlyOverride() {
    return straightLineMonthlyOverride;
  }

  public FixedAssetStatus getStatus() {
    return status;
  }
}
