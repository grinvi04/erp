package com.erp.common.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** 관측성 필터 등록. {@link TraceIdFilter}를 보안 필터보다 먼저 돌도록 최우선 순위로 등록해 인증 실패 응답에도 traceId가 붙게 한다. */
@Configuration
public class ObservabilityConfig {

  @Bean
  public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
    FilterRegistrationBean<TraceIdFilter> registration =
        new FilterRegistrationBean<>(new TraceIdFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
