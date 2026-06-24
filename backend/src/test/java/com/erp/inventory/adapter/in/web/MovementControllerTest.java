package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.MovementCreateRequest;
import com.erp.inventory.application.dto.MovementLineRequest;
import com.erp.inventory.application.dto.MovementResponse;
import com.erp.inventory.application.service.MovementService;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(MovementController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class MovementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MovementService movementService;

    private MovementResponse buildResponse(MovementStatus status) {
        return new MovementResponse(1L, "MOV-20260624-11111", MovementType.RECEIPT,
                status, null, null, LocalDate.of(2026, 6, 24), "입고", List.of());
    }

    @Test
    void findAll_returnsOkWithPage() throws Exception {
        PageResponse<MovementResponse> page = new PageResponse<>(
                List.of(buildResponse(MovementStatus.DRAFT)), 0, 10, 1, 1, true, true);
        given(movementService.findAll(any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/inventory/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].movementNo").value("MOV-20260624-11111"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(movementService.create(any())).willReturn(buildResponse(MovementStatus.DRAFT));

        MovementCreateRequest req = new MovementCreateRequest(
                MovementType.RECEIPT, LocalDate.of(2026, 6, 24), null, null, "입고",
                List.of(new MovementLineRequest(1L, null, 2L, null, null,
                        BigDecimal.TEN, BigDecimal.ONE)));

        mockMvc.perform(post("/api/inventory/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void confirm_validId_returnsConfirmed() throws Exception {
        given(movementService.confirm(1L)).willReturn(buildResponse(MovementStatus.CONFIRMED));

        mockMvc.perform(post("/api/inventory/movements/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirm_insufficientStock_returns409() throws Exception {
        given(movementService.confirm(1L)).willThrow(new ErpException(ErrorCode.INSUFFICIENT_STOCK));

        mockMvc.perform(post("/api/inventory/movements/1/confirm"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_validId_returnsCancelled() throws Exception {
        given(movementService.cancel(1L)).willReturn(buildResponse(MovementStatus.CANCELLED));

        mockMvc.perform(post("/api/inventory/movements/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
