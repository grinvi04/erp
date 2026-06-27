package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 영업팀(CRM 영업조직) — DataScope의 DEPARTMENT(팀) 스코프 기준. 팀원(사용자 sub)을 묶어, DEPARTMENT 스코프 사용자가 같은 팀 팀원들의
 * 영업데이터(owner 기준)를 볼 수 있게 한다. 부서(HR)와 독립한 CRM 자체 평면 팀 모델 — 모듈 경계 유지.
 */
@Entity
@Table(name = "sales_team", schema = "crm")
public class SalesTeam extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sales_team_seq")
  @SequenceGenerator(
      name = "sales_team_seq",
      sequenceName = "crm.sales_team_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @OneToMany(
      mappedBy = "team",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<SalesTeamMember> members = new HashSet<>();

  protected SalesTeam() {}

  public static SalesTeam of(String code, String name) {
    SalesTeam t = new SalesTeam();
    t.code = code;
    t.name = name;
    return t;
  }

  public void rename(String name) {
    this.name = name;
  }

  /** 팀원 추가(멱등 — 이미 있으면 무시). */
  public void addMember(String userId) {
    boolean exists = members.stream().anyMatch(m -> m.getUserId().equals(userId));
    if (!exists) {
      members.add(SalesTeamMember.of(this, userId));
    }
  }

  public void removeMember(String userId) {
    members.removeIf(m -> m.getUserId().equals(userId));
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

  public Set<String> getMemberUserIds() {
    return members.stream().map(SalesTeamMember::getUserId).collect(Collectors.toUnmodifiableSet());
  }
}
