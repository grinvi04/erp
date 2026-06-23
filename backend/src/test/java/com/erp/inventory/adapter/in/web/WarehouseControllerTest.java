package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.WarehouseCreateRequest;
import com.erp.inventory.application.dto.WarehouseResponse;
import com.erp.inventory.application.service.WarehouseService;
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

@WebMvcTest(WarehouseController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class WarehouseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WarehouseService warehouseService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        given(warehouseService.findAll()).willReturn(
                List.of(new WarehouseResponse(1L, "WH-001", "본창고", "서울", true)));

        mockMvc.perform(get("/api/inventory/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("WH-001"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(warehouseService.create(any())).willReturn(
                new WarehouseResponse(1L, "WH-001", "본창고", "서울", true));

        mockMvc.perform(post("/api/inventory/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WarehouseCreateRequest("WH-001", "본창고", "서울"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("WH-001"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(warehouseService.create(any()))
                .willThrow(new ErpException(ErrorCode.WAREHOUSE_CODE_DUPLICATE));

        mockMvc.perform(post("/api/inventory/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WarehouseCreateRequest("WH-001", "본창고", "서울"))))
                .andExpect(status().isConflict());
    }
}
