package com.erp.common.audit;

import com.erp.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
@RequiredArgsConstructor
public class AuditorProvider implements AuditorAware<String> {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(currentUserProvider.getCurrentUserId())
                .or(() -> Optional.of("system"));
    }
}
