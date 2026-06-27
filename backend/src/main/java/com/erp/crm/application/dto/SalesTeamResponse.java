package com.erp.crm.application.dto;

import com.erp.crm.domain.model.SalesTeam;
import java.util.Set;

public record SalesTeamResponse(
    Long id, String code, String name, Set<String> memberUserIds, Long version) {
  public static SalesTeamResponse from(SalesTeam t) {
    return new SalesTeamResponse(
        t.getId(), t.getCode(), t.getName(), t.getMemberUserIds(), t.getVersion());
  }
}
