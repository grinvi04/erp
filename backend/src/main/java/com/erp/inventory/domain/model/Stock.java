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
import java.math.RoundingMode;

@Entity
@Table(name = "stock", schema = "inventory")
public class Stock extends BaseEntity {

    private static final int COST_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_seq")
    @SequenceGenerator(name = "stock_seq", sequenceName = "inventory.stock_id_seq", allocationSize = 100)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, updatable = false)
    private Location location;

    @Column(name = "lot_no", length = 50)
    private String lotNo;

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(name = "qty_on_hand", nullable = false, precision = 12, scale = 2)
    private BigDecimal qtyOnHand;

    @Column(name = "qty_reserved", nullable = false, precision = 12, scale = 2)
    private BigDecimal qtyReserved;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    protected Stock() {}

    public static Stock create(Item item, Location location, String lotNo, String serialNo, BigDecimal unitCost) {
        Stock s = new Stock();
        s.item = item;
        s.location = location;
        s.lotNo = lotNo;
        s.serialNo = serialNo;
        s.qtyOnHand = BigDecimal.ZERO;
        s.qtyReserved = BigDecimal.ZERO;
        s.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        return s;
    }

    public void increase(BigDecimal qty, BigDecimal incomingCost) {
        BigDecimal totalValue = this.qtyOnHand.multiply(this.unitCost)
                .add(qty.multiply(incomingCost));
        this.qtyOnHand = this.qtyOnHand.add(qty);
        if (this.qtyOnHand.compareTo(BigDecimal.ZERO) > 0) {
            this.unitCost = totalValue.divide(this.qtyOnHand, COST_SCALE, RoundingMode.HALF_UP);
        }
    }

    public void decrease(BigDecimal qty) {
        if (this.qtyOnHand.compareTo(qty) < 0) {
            throw new ErpException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.qtyOnHand = this.qtyOnHand.subtract(qty);
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public Location getLocation() { return location; }
    public String getLotNo() { return lotNo; }
    public String getSerialNo() { return serialNo; }
    public BigDecimal getQtyOnHand() { return qtyOnHand; }
    public BigDecimal getQtyReserved() { return qtyReserved; }
    public BigDecimal getUnitCost() { return unitCost; }
}
