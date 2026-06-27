package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.VendorCreateRequest;
import com.erp.finance.application.dto.VendorResponse;
import com.erp.finance.application.service.VendorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VendorController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class VendorControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private VendorService vendorService;

  @Test
  void findAll_returnsOkWithPage() throws Exception {
    VendorResponse response =
        new VendorResponse(1L, "V001", "공급사", null, null, null, null, 30, true, null, 0L);
    given(vendorService.findAll(any(), any()))
        .willReturn(PageResponse.from(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1)));

    mockMvc
        .perform(get("/api/finance/vendors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].code").value("V001"));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    VendorCreateRequest request =
        new VendorCreateRequest("V001", "공급사", null, null, null, null, 30, null);
    VendorResponse response =
        new VendorResponse(1L, "V001", "공급사", null, null, null, null, 30, true, null, 0L);
    given(vendorService.create(any())).willReturn(response);

    mockMvc
        .perform(
            post("/api/finance/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.code").value("V001"));
  }

  @Test
  void create_duplicateCode_returns409() throws Exception {
    given(vendorService.create(any())).willThrow(new ErpException(ErrorCode.VENDOR_CODE_DUPLICATE));

    mockMvc
        .perform(
            post("/api/finance/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new VendorCreateRequest("V001", "공급사", null, null, null, null, 30, null))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }
}
