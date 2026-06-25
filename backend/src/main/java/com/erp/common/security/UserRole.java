package com.erp.common.security;

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

/**
 * 사용자(Keycloak sub) ↔ 역할 배정. 한 사용자가 여러 역할을 가질 수 있다.
 * tenant_id 명시 필터링({@link Role} 참조).
 */
@Entity
@Table(name = "user_role", schema = "common")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_role_seq")
    @SequenceGenerator(name = "user_role_seq", sequenceName = "common.user_role_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    protected UserRole() {}

    public static UserRole of(Long tenantId, String userId, Role role) {
        UserRole ur = new UserRole();
        ur.tenantId = tenantId;
        ur.userId = userId;
        ur.role = role;
        return ur;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public Role getRole() { return role; }
}
