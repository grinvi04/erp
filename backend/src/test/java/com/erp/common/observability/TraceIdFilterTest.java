package com.erp.common.observability;

import org.junit.jupiter.api.Test;

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
