package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** 직위 (사원, 대리, 과장, 차장, 부장 등). */
@Entity
@Table(name = "position", schema = "hr")
public class Position extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "position_seq")
  @SequenceGenerator(
      name = "position_seq",
      sequenceName = "hr.position_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "level_order", nullable = false)
  private int levelOrder;

  protected Position() {}

  public static Position of(String code, String name, int levelOrder) {
    Position p = new Position();
    p.code = code;
    p.name = name;
    p.levelOrder = levelOrder;
    return p;
  }

  public void update(String name, int levelOrder) {
    this.name = name;
    this.levelOrder = levelOrder;
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

  public int getLevelOrder() {
    return levelOrder;
  }
}
