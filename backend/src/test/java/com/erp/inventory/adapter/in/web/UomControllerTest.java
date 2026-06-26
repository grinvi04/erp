package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.UomCreateRequest;
import com.erp.inventory.application.dto.UomResponse;
import com.erp.inventory.application.service.UomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UomController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UomControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UomService uomService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        given(uomService.findAll()).willReturn(List.of(new UomResponse(1L, "EA", "개", 0L)));

        mockMvc.perform(get("/api/inventory/uoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("EA"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(uomService.create(any())).willReturn(new UomResponse(1L, "EA", "개", 0L));

        mockMvc.perform(post("/api/inventory/uoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UomCreateRequest("EA", "개"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("EA"));
    }

    @Test
    void create_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/inventory/uoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UomCreateRequest("", "개"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(uomService.findById(99L)).willThrow(new ErpException(ErrorCode.UOM_NOT_FOUND));

        mockMvc.perform(get("/api/inventory/uoms/99"))
                .andExpect(status().isNotFound());
    }
}
