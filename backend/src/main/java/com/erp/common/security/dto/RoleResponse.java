package com.erp.common.security.dto;

import com.erp.common.security.Role;
import java.util.Set;
import java.util.TreeSet;

public record RoleResponse(
    Long id, String code, String name, String description, Set<String> permissions) {
  public static RoleResponse from(Role role) {
    return new RoleResponse(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDescription(),
        new TreeSet<>(role.getPermissions()));
  }
}
