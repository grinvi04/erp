package com.erp.common.security;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionCheckerTest {

    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String... authorities) {
        var granted = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var token = new UsernamePasswordAuthenticationToken("user", "n/a", granted);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void require_withMatchingAuthority_passes() {
        authenticate(Permission.HR_EMPLOYEE_WRITE, Permission.HR_EMPLOYEE_READ);
        assertThatNoException().isThrownBy(() -> checker.require(Permission.HR_EMPLOYEE_WRITE));
    }

    @Test
    void require_withoutAuthority_throwsForbidden() {
        authenticate(Permission.HR_EMPLOYEE_READ); // read만 있고 write 없음
        ErpException ex = assertThrows(ErpException.class,
                () -> checker.require(Permission.HR_EMPLOYEE_WRITE));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void require_unauthenticated_throwsForbidden() {
        SecurityContextHolder.clearContext();
        ErpException ex = assertThrows(ErpException.class,
                () -> checker.require(Permission.HR_EMPLOYEE_READ));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void currentPermissions_returnsAuthoritySet() {
        authenticate(Permission.HR_EMPLOYEE_READ, Permission.FINANCE_READ);
        assertThat(checker.currentPermissions())
                .containsExactlyInAnyOrder(Permission.HR_EMPLOYEE_READ, Permission.FINANCE_READ);
    }
}
