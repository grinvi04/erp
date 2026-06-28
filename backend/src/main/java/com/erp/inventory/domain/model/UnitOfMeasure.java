package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "unit_of_measure", schema = "inventory")
@SQLRestriction("deleted_at IS NULL")
public class UnitOfMeasure extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uom_seq")
  @SequenceGenerator(name = "uom_seq", sequenceName = "inventory.uom_id_seq", allocationSize = 20)
  private Long id;

  @Column(name = "code", nullable = false, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  protected UnitOfMeasure() {}

  public static UnitOfMeasure of(String code, String name) {
    UnitOfMeasure uom = new UnitOfMeasure();
    uom.code = code.toUpperCase();
    uom.name = name;
    return uom;
  }

  public void update(String name) {
    this.name = name;
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
}
