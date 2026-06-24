package com.erp.common.config;

import com.erp.common.tenant.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateConfig {

    private final TenantIdentifierResolver tenantIdentifierResolver;

    /**
     * TenantIdentifierResolver를 Spring 빈으로 Hibernate에 주입.
     * application.yml 문자열 등록 시 Hibernate가 직접 인스턴스화해서
     * TenantContext(ThreadLocal)를 읽지 못하는 문제를 방지한다.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> properties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver
        );
    }
}
