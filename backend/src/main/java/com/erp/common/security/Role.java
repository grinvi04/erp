package com.erp.common.security;

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
 * 역할 — 권한 코드의 묶음(운영이 관리 화면에서 조합). 코드는 권한을 검사하고, 역할은 그
 * 권한 집합을 사용자에게 부여하는 단위다(auth-standards). {@link com.erp.common.audit.AuditLog}와
 * 동일하게 {@code BaseEntity}/@TenantId를 쓰지 않고 tenant_id를 명시 필터링한다 — 권한 해석이
 * JWT 인증 단계(TenantContext 세팅 전)에서 일어나기 때문.
 */
@Entity
@Table(name = "role", schema = "common")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "common.role_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true,
        fetch = FetchType.LAZY)
    private Set<RolePermission> permissions = new HashSet<>();

    protected Role() {}

    public static Role of(Long tenantId, String code, String name, String description) {
        Role role = new Role();
        role.tenantId = tenantId;
        role.code = code;
        role.name = name;
        role.description = description;
        return role;
    }

    public void grant(String permissionCode) {
        boolean already = permissions.stream()
            .anyMatch(p -> p.getPermissionCode().equals(permissionCode));
        if (!already) {
            permissions.add(RolePermission.of(tenantId, this, permissionCode));
        }
    }

    public void revoke(String permissionCode) {
        permissions.removeIf(p -> p.getPermissionCode().equals(permissionCode));
    }

    /**
     * 권한 집합을 주어진 코드들로 교체한다. 사라진 코드만 제거하고 새 코드만 추가해
     * 유지되는 행은 건드리지 않는다 — 삭제 후 재삽입이 (role_id, permission_code) UNIQUE를
     * 일시적으로 위반하는 것을 피한다.
     */
    public void replacePermissions(Set<String> permissionCodes) {
        Set<String> target = permissionCodes == null ? Set.of() : permissionCodes;
        permissions.removeIf(p -> !target.contains(p.getPermissionCode()));
        target.forEach(this::grant);
    }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<String> getPermissions() {
        return permissions.stream().map(RolePermission::getPermissionCode)
            .collect(Collectors.toUnmodifiableSet());
    }
}
