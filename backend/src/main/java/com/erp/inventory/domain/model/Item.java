package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
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

@Entity
@Table(name = "item", schema = "inventory")
public class Item extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_seq")
  @SequenceGenerator(name = "item_seq", sequenceName = "inventory.item_id_seq", allocationSize = 50)
  private Long id;

  @Column(name = "sku", nullable = false, length = 50)
  private String sku;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private ItemCategory category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uom_id", nullable = false)
  private UnitOfMeasure uom;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_method", nullable = false, length = 20)
  private CostMethod costMethod;

  @Column(name = "standard_cost", nullable = false, precision = 15, scale = 4)
  private BigDecimal standardCost;

  @Column(name = "reorder_point", nullable = false, precision = 12, scale = 2)
  private BigDecimal reorderPoint;

  @Column(name = "reorder_qty", nullable = false, precision = 12, scale = 2)
  private BigDecimal reorderQty;

  @Column(name = "min_stock", nullable = false, precision = 12, scale = 2)
  private BigDecimal minStock;

  @Column(name = "max_stock", nullable = false, precision = 12, scale = 2)
  private BigDecimal maxStock;

  @Column(name = "is_lot_tracked", nullable = false)
  private boolean lotTracked;

  @Column(name = "is_serial_tracked", nullable = false)
  private boolean serialTracked;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected Item() {}

  public static Item of(
      String sku,
      String name,
      String description,
      ItemCategory category,
      UnitOfMeasure uom,
      CostMethod costMethod,
      BigDecimal standardCost,
      BigDecimal reorderPoint,
      BigDecimal reorderQty,
      BigDecimal minStock,
      BigDecimal maxStock,
      boolean lotTracked,
      boolean serialTracked) {
    Item item = new Item();
    item.sku = sku.toUpperCase();
    item.name = name;
    item.description = description;
    item.category = category;
    item.uom = uom;
    item.costMethod = costMethod;
    item.standardCost = standardCost;
    item.reorderPoint = reorderPoint;
    item.reorderQty = reorderQty;
    item.minStock = minStock;
    item.maxStock = maxStock;
    item.lotTracked = lotTracked;
    item.serialTracked = serialTracked;
    item.active = true;
    return item;
  }

  public void update(
      String name,
      String description,
      ItemCategory category,
      UnitOfMeasure uom,
      CostMethod costMethod,
      BigDecimal standardCost,
      BigDecimal reorderPoint,
      BigDecimal reorderQty,
      BigDecimal minStock,
      BigDecimal maxStock,
      boolean lotTracked,
      boolean serialTracked) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.uom = uom;
    this.costMethod = costMethod;
    this.standardCost = standardCost;
    this.reorderPoint = reorderPoint;
    this.reorderQty = reorderQty;
    this.minStock = minStock;
    this.maxStock = maxStock;
    this.lotTracked = lotTracked;
    this.serialTracked = serialTracked;
  }

  public void deactivate() {
    this.active = false;
  }

  public void activate() {
    this.active = true;
  }

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public UnitOfMeasure getUom() {
    return uom;
  }

  public CostMethod getCostMethod() {
    return costMethod;
  }

  public BigDecimal getStandardCost() {
    return standardCost;
  }

  public BigDecimal getReorderPoint() {
    return reorderPoint;
  }

  public BigDecimal getReorderQty() {
    return reorderQty;
  }

  public BigDecimal getMinStock() {
    return minStock;
  }

  public BigDecimal getMaxStock() {
    return maxStock;
  }

  public boolean isLotTracked() {
    return lotTracked;
  }

  public boolean isSerialTracked() {
    return serialTracked;
  }

  public boolean isActive() {
    return active;
  }
}
