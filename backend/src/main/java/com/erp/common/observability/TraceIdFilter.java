package com.erp.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 상관관계(traceId) 관측성 필터.
 *
 * <p>인바운드 W3C Trace Context {@code traceparent} 헤더에서 trace-id를 추출해 재사용하고, 없거나 형식이 유효하지 않으면 새로 생성한다.
 * trace-id는 MDC에 넣어 모든 로그에 자동 포함되게 하고, 응답 {@code X-Trace-Id} 헤더로 반환해 클라이언트가 역추적할 수 있게 한다.
 *
 * <p>모든 요청에 최우선(HIGHEST_PRECEDENCE) 적용되어 인증 실패 응답에도 traceId가 붙는다.
 */
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String TRACEPARENT_HEADER = "traceparent";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String MDC_TRACE_ID = "traceId";

  /** 해석한 traceId를 ASYNC 재디스패치 간 공유하기 위한 request attribute 키. */
  private static final String ATTR_TRACE_ID = TraceIdFilter.class.getName() + ".traceId";

  /** W3C trace-id 길이 (16 bytes → 32 hex chars). */
  private static final int TRACE_ID_LENGTH = 32;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // ASYNC 재디스패치에서도 같은 traceId를 쓰도록 request attribute에 캐시.
    // traceparent 없는 요청이 재디스패치마다 다른 난수를 생성하는 것을 방지한다.
    String traceId = (String) request.getAttribute(ATTR_TRACE_ID);
    if (traceId == null) {
      traceId = resolveTraceId(request.getHeader(TRACEPARENT_HEADER));
      request.setAttribute(ATTR_TRACE_ID, traceId);
    }
    MDC.put(MDC_TRACE_ID, traceId);
    response.setHeader(TRACE_ID_HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_TRACE_ID);
    }
  }

  /**
   * W3C {@code traceparent} 헤더에서 trace-id(32 hex)를 추출한다. 형식: {@code version-traceid-spanid-flags}
   * (예 {@code 00-4bf92f35...-00f0...-01}). 유효한 trace-id(32 hex, all-zero 아님)가 있으면 그대로 쓰고, 아니면 새로
   * 생성한다.
   */
  String resolveTraceId(String traceparent) {
    if (traceparent != null) {
      String[] parts = traceparent.split("-");
      if (parts.length >= 2) {
        String traceId = parts[1];
        if (isValidTraceId(traceId)) {
          // W3C 규정: trace-id는 소문자 hex. 대문자 인바운드도 정규화해 재사용.
          return traceId.toLowerCase();
        }
      }
    }
    return generateTraceId();
  }

  private boolean isValidTraceId(String traceId) {
    if (traceId.length() != TRACE_ID_LENGTH) {
      return false;
    }
    boolean allZero = true;
    for (int i = 0; i < traceId.length(); i++) {
      char c = traceId.charAt(i);
      boolean isHex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
      if (!isHex) {
        return false;
      }
      if (c != '0') {
        allZero = false;
      }
    }
    return !allZero;
  }

  private String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
