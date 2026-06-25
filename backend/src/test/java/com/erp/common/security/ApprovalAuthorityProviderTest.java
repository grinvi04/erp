package com.erp.common.security;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApprovalAuthorityProviderTest {

    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AuthorizationResolver authorizationResolver;

    @InjectMocks
    private ApprovalAuthorityProvider provider;

    private void currentUser() {
        given(currentUserProvider.getCurrentTenantId()).willReturn(1L);
        given(currentUserProvider.getCurrentUserId()).willReturn("u");
    }

    @Test
    void getApprovalLimit_fromProfile() {
        currentUser();
        given(authorizationResolver.accessProfile(1L, "u")).willReturn(Optional.of(
                UserAccessProfile.of(1L, "u", DataScope.ALL, null, new BigDecimal("1000000"))));

        assertThat(provider.getApprovalLimit()).isEqualByComparingTo("1000000");
    }

    @Test
    void getApprovalLimit_noProfile_zero() {
        currentUser();
        given(authorizationResolver.accessProfile(1L, "u")).willReturn(Optional.empty());

        assertThat(provider.getApprovalLimit()).isEqualByComparingTo("0");
    }

    @Test
    void getApprovalLimit_nullLimitOnProfile_zero() {
        currentUser();
        given(authorizationResolver.accessProfile(1L, "u")).willReturn(Optional.of(
                UserAccessProfile.of(1L, "u", DataScope.ALL, null, null)));

        assertThat(provider.getApprovalLimit()).isEqualByComparingTo("0");
    }
}
