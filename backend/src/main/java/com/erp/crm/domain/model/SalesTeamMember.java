package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
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

/** 영업팀 멤버 — 팀(SalesTeam)에 속한 사용자(sub). 반드시 부모 {@link SalesTeam}을 통해 추가/제거한다. */
@Entity
@Table(name = "sales_team_member", schema = "crm")
public class SalesTeamMember extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sales_team_member_seq")
  @SequenceGenerator(
      name = "sales_team_member_seq",
      sequenceName = "crm.sales_team_member_id_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private SalesTeam team;

  @Column(name = "user_id", nullable = false, length = 100)
  private String userId;

  protected SalesTeamMember() {}

  static SalesTeamMember of(SalesTeam team, String userId) {
    SalesTeamMember m = new SalesTeamMember();
    m.team = team;
    m.userId = userId;
    return m;
  }

  public Long getId() {
    return id;
  }

  public SalesTeam getTeam() {
    return team;
  }

  public String getUserId() {
    return userId;
  }
}
