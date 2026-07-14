package com.erp.common.exception;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.observability.ObservabilityConfig;
import com.erp.common.observability.TraceIdFilter;
import com.erp.finance.adapter.in.web.ApInvoiceController;
import com.erp.finance.application.service.ApInvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 지원하지 않는 HTTP 메서드의 공통 오류 응답과 관측성 계약. */
@WebMvcTest(ApInvoiceController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, ObservabilityConfig.class})
class HttpMethodNotAllowedIntegrationTest {

  private static final String RESOURCE_PATH = "/api/finance/invoices/42";
  private static final String EXPECTED_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

  @Autowired private MockMvc mockMvc;
  @MockBean private ApInvoiceService apInvoiceService;

  @Test
  void unsupportedMethod_anonymous_returns405EnvelopeAllowAndGeneratedTraceId() throws Exception {
    mockMvc
        .perform(post(RESOURCE_PATH))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string(HttpHeaders.ALLOW, HttpMethod.GET.name()))
        .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, matchesPattern("[0-9a-f]{32}")))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error.code").value("C007"));
  }

  @Test
  void unsupportedMethod_authenticated_preservesInboundTraceIdAndSamePolicy() throws Exception {
    mockMvc
        .perform(
            post(RESOURCE_PATH)
                .with(jwt())
                .header(
                    TraceIdFilter.TRACEPARENT_HEADER,
                    "00-" + EXPECTED_TRACE_ID + "-00f067aa0ba902b7-01"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string(HttpHeaders.ALLOW, HttpMethod.GET.name()))
        .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, EXPECTED_TRACE_ID))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error.code").value("C007"));
  }
}
