package com.erp.common.security;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DataScopeProviderTest {

    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AuthorizationResolver authorizationResolver;

    @InjectMocks
    private DataScopeProvider provider;

    private void currentUser(Long tenantId, String userId) {
        given(currentUserProvider.getCurrentTenantId()).willReturn(tenantId);
        given(currentUserProvider.getCurrentUserId()).willReturn(userId);
    }

    @Test
    void getDataScope_fromProfile() {
        currentUser(1L, "u");
        given(authorizationResolver.accessProfile(1L, "u")).willReturn(Optional.of(
                UserAccessProfile.of(1L, "u", DataScope.DEPARTMENT, 5L, null)));

        assertThat(provider.getDataScope()).isEqualTo(DataScope.DEPARTMENT);
        assertThat(provider.getDepartmentId()).isEqualTo(5L);
    }

    @Test
    void getDataScope_noProfile_defaultsToAll() {
        currentUser(1L, "u");
        given(authorizationResolver.accessProfile(1L, "u")).willReturn(Optional.empty());

        assertThat(provider.getDataScope()).isEqualTo(DataScope.ALL);
        assertThat(provider.getDepartmentId()).isNull();
    }

    @Test
    void getDataScope_unauthenticated_defaultsToAll() {
        currentUser(null, null);
        given(authorizationResolver.accessProfile(null, null)).willReturn(Optional.empty());

        assertThat(provider.getDataScope()).isEqualTo(DataScope.ALL);
    }
}
