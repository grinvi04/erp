package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "movement_line", schema = "inventory")
@SQLRestriction("deleted_at IS NULL")
public class MovementLine extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movement_line_seq")
  @SequenceGenerator(
      name = "movement_line_seq",
      sequenceName = "inventory.movement_line_id_seq",
      allocationSize = 500)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movement_id", nullable = false, updatable = false)
  private Movement movement;

  @Column(name = "line_no", nullable = false)
  private Integer lineNo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false, updatable = false)
  private Item item;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_location_id")
  private Location fromLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_location_id")
  private Location toLocation;

  @Column(name = "lot_no", length = 50)
  private String lotNo;

  @Column(name = "serial_no", length = 100)
  private String serialNo;

  @Column(name = "qty", nullable = false, precision = 12, scale = 2)
  private BigDecimal qty;

  @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
  private BigDecimal unitCost;

  protected MovementLine() {}

  public static MovementLine of(
      Movement movement,
      Integer lineNo,
      Item item,
      Location fromLocation,
      Location toLocation,
      String lotNo,
      String serialNo,
      BigDecimal qty,
      BigDecimal unitCost) {
    if (fromLocation == null && toLocation == null) {
      throw new ErpException(ErrorCode.INVALID_INPUT);
    }
    if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT);
    }
    MovementLine line = new MovementLine();
    line.movement = movement;
    line.lineNo = lineNo;
    line.item = item;
    line.fromLocation = fromLocation;
    line.toLocation = toLocation;
    line.lotNo = lotNo;
    line.serialNo = serialNo;
    line.qty = qty;
    line.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
    return line;
  }

  public Long getId() {
    return id;
  }

  public Movement getMovement() {
    return movement;
  }

  public Integer getLineNo() {
    return lineNo;
  }

  public Item getItem() {
    return item;
  }

  public Location getFromLocation() {
    return fromLocation;
  }

  public Location getToLocation() {
    return toLocation;
  }

  public String getLotNo() {
    return lotNo;
  }

  public String getSerialNo() {
    return serialNo;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }
}
