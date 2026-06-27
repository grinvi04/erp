package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse", schema = "inventory")
public class Warehouse extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouse_seq")
  @SequenceGenerator(
      name = "warehouse_seq",
      sequenceName = "inventory.warehouse_id_seq",
      allocationSize = 20)
  private Long id;

  @Column(name = "code", nullable = false, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected Warehouse() {}

  public static Warehouse of(String code, String name, String address) {
    Warehouse w = new Warehouse();
    w.code = code.toUpperCase();
    w.name = name;
    w.address = address;
    w.active = true;
    return w;
  }

  public void update(String name, String address) {
    this.name = name;
    this.address = address;
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

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public boolean isActive() {
    return active;
  }
}
