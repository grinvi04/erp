package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.PositionCreateRequest;
import com.erp.hr.application.dto.PositionResponse;
import com.erp.hr.application.service.PositionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PositionService positionService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        given(positionService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/hr/positions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        PositionCreateRequest request = new PositionCreateRequest("MGR", "과장", 3);
        PositionResponse response = new PositionResponse(1L, "MGR", "과장", 3);
        given(positionService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/hr/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").value("MGR"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        PositionCreateRequest request = new PositionCreateRequest("MGR", "과장", 3);
        given(positionService.create(any())).willThrow(new ErpException(ErrorCode.DUPLICATE_CODE));

        mockMvc.perform(post("/api/hr/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H011"));
    }

    @Test
    void delete_positionInUse_returns409() throws Exception {
        willThrow(new ErpException(ErrorCode.POSITION_IN_USE))
            .given(positionService).delete(1L);

        mockMvc.perform(delete("/api/hr/positions/{id}", 1L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H013"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/hr/positions/{id}", 1L))
            .andExpect(status().isNoContent());
    }
}
