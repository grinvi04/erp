package com.erp.common.observability;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void validTraceparent_returnsItsTraceId() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String traceparent = "00-" + traceId + "-00f067aa0ba902b7-01";

        assertThat(filter.resolveTraceId(traceparent)).isEqualTo(traceId);
    }

    @Test
    void uppercaseTraceparent_normalizedToLowercase() {
        String upper = "4BF92F3577B34DA6A3CE929D0E0E4736";
        String traceparent = "00-" + upper + "-00F067AA0BA902B7-01";

        assertThat(filter.resolveTraceId(traceparent)).isEqualTo(upper.toLowerCase());
    }

    @Test
    void sameRequest_reusesCachedTraceIdAcrossDispatches() throws ServletException, IOException {
        // traceparent 없는 요청 — 캐시가 없으면 디스패치마다 다른 난수가 생성된다.
        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilterInternal(request, first, new MockFilterChain());
        String firstTraceId = first.getHeader(TraceIdFilter.TRACE_ID_HEADER);

        // 같은 request 객체로 재디스패치 — request attribute 캐시를 재사용해야 한다.
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilterInternal(request, second, new MockFilterChain());
        String secondTraceId = second.getHeader(TraceIdFilter.TRACE_ID_HEADER);

        assertThat(firstTraceId).matches("[0-9a-f]{32}");
        assertThat(secondTraceId).isEqualTo(firstTraceId);
    }

    @Test
    void allZeroTraceId_generatesNew() {
        String traceparent = "00-00000000000000000000000000000000-00f067aa0ba902b7-01";

        String result = filter.resolveTraceId(traceparent);

        assertThat(result).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(result).isNotEqualTo("00000000000000000000000000000000");
    }

    @Test
    void nullTraceparent_generatesNew() {
        String result = filter.resolveTraceId(null);

        assertThat(result).hasSize(32).matches("[0-9a-f]{32}");
    }

    @Test
    void malformedTraceparent_generatesNew() {
        assertThat(filter.resolveTraceId("garbage")).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(filter.resolveTraceId("00-tooshort-x-01")).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(filter.resolveTraceId("00-zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz-00f0-01"))
                .hasSize(32).matches("[0-9a-f]{32}");
    }

    @Test
    void generatedTraceId_is32HexChars() {
        String result = filter.resolveTraceId(null);

        assertThat(result).matches("[0-9a-f]{32}");
    }
}
