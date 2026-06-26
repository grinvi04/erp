package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.PipelineStageCreateRequest;
import com.erp.crm.application.dto.PipelineStageResponse;
import com.erp.crm.application.service.PipelineStageService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineStageController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PipelineStageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PipelineStageService stageService;

    private PipelineStageResponse buildResponse() {
        return new PipelineStageResponse(1L, "탐색", 1, 20, false, false);
    }

    private PipelineStageCreateRequest validCreate() {
        return new PipelineStageCreateRequest("탐색", 1, 20, false, false);
    }

    @Test
    void findAll_returnsOk() throws Exception {
        given(stageService.findAll()).willReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/crm/pipeline-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("탐색"))
                .andExpect(jsonPath("$.data[0].stageOrder").value(1));
    }

    @Test
    void findById_existingStage_returnsOk() throws Exception {
        given(stageService.findById(1L)).willReturn(buildResponse());

        mockMvc.perform(get("/api/crm/pipeline-stages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("탐색"))
                .andExpect(jsonPath("$.data.probability").value(20));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(stageService.findById(99L))
                .willThrow(new ErpException(ErrorCode.PIPELINE_STAGE_NOT_FOUND));

        mockMvc.perform(get("/api/crm/pipeline-stages/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CR004"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(stageService.create(any())).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/pipeline-stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("탐색"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        PipelineStageCreateRequest invalid =
                new PipelineStageCreateRequest("", 1, 20, false, false);

        mockMvc.perform(post("/api/crm/pipeline-stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidStageOrder_returns400() throws Exception {
        PipelineStageCreateRequest invalid =
                new PipelineStageCreateRequest("탐색", 0, 20, false, false);

        mockMvc.perform(post("/api/crm/pipeline-stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_probabilityOutOfRange_returns400() throws Exception {
        PipelineStageCreateRequest invalid =
                new PipelineStageCreateRequest("탐색", 1, 150, false, false);

        mockMvc.perform(post("/api/crm/pipeline-stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validRequest_returnsOk() throws Exception {
        given(stageService.update(eq(1L), any())).willReturn(buildResponse());

        mockMvc.perform(put("/api/crm/pipeline-stages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        given(stageService.update(eq(99L), any()))
                .willThrow(new ErpException(ErrorCode.PIPELINE_STAGE_NOT_FOUND));

        mockMvc.perform(put("/api/crm/pipeline-stages/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingStage_returns204() throws Exception {
        mockMvc.perform(delete("/api/crm/pipeline-stages/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        given(stageService.create(any()))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/crm/pipeline-stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }

    @Test
    void findById_noPermission_returns403() throws Exception {
        given(stageService.findById(1L))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/crm/pipeline-stages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        willThrow(new ErpException(ErrorCode.FORBIDDEN))
                .given(stageService).delete(1L);

        mockMvc.perform(delete("/api/crm/pipeline-stages/1"))
                .andExpect(status().isForbidden());
    }
}
