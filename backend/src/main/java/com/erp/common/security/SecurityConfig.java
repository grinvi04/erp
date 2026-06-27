package com.erp.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

  private final JwtTenantFilter jwtTenantFilter;
  private final AuthorizationResolver authorizationResolver;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        // JWT 인증 완료 후 tenant 추출
        .addFilterAfter(jwtTenantFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  /**
   * {@link JwtTenantFilter}는 {@code @Component}라 서블릿 컨테이너에 자동등록되어 시큐리티 체인({@code addFilterAfter})
   * 등록과 이중 실행된다. 서블릿 자동등록을 비활성화해 시큐리티 체인에서만 실행되게 한다.
   */
  @Bean
  public FilterRegistrationBean<JwtTenantFilter> jwtTenantFilterRegistration(
      JwtTenantFilter filter) {
    FilterRegistrationBean<JwtTenantFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /** JWT 신원 → DB(역할→권한) 기반 authority 변환. */
  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter(authorizationResolver));
    return converter;
  }
}
