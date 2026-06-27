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

@Entity
@Table(name = "location", schema = "inventory")
public class Location extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_seq")
  @SequenceGenerator(
      name = "location_seq",
      sequenceName = "inventory.location_id_seq",
      allocationSize = 100)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Location parent;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_type", nullable = false, length = 20)
  private LocationType locationType;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected Location() {}

  public static Location of(
      Warehouse warehouse, String code, String name, Location parent, LocationType locationType) {
    Location loc = new Location();
    loc.warehouse = warehouse;
    loc.code = code.toUpperCase();
    loc.name = name;
    loc.parent = parent;
    loc.locationType = locationType;
    loc.active = true;
    return loc;
  }

  public void update(String name, Location parent, LocationType locationType) {
    this.name = name;
    this.parent = parent;
    this.locationType = locationType;
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

  public Warehouse getWarehouse() {
    return warehouse;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Location getParent() {
    return parent;
  }

  public LocationType getLocationType() {
    return locationType;
  }

  public boolean isActive() {
    return active;
  }
}
