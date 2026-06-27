package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.LocationCreateRequest;
import com.erp.inventory.application.dto.LocationResponse;
import com.erp.inventory.application.service.LocationService;
import com.erp.inventory.domain.model.LocationType;
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

@WebMvcTest(LocationController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LocationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LocationService locationService;

    private LocationResponse buildResponse() {
        return new LocationResponse(1L, 1L, "본창고", "A-01", "구역A", null, null, LocationType.ZONE, true, 0L);
    }

    @Test
    void findByWarehouse_returnsOkWithList() throws Exception {
        given(locationService.findByWarehouse(1L)).willReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/inventory/locations").param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("A-01"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(locationService.create(any())).willReturn(buildResponse());

        mockMvc.perform(post("/api/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LocationCreateRequest(1L, "A-01", "구역A", null, LocationType.ZONE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("A-01"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(locationService.create(any()))
                .willThrow(new ErpException(ErrorCode.LOCATION_CODE_DUPLICATE));

        mockMvc.perform(post("/api/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LocationCreateRequest(1L, "A-01", "구역A", null, LocationType.ZONE))))
                .andExpect(status().isConflict());
    }
}
