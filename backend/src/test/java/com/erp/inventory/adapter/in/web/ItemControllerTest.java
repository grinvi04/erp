package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.ItemCreateRequest;
import com.erp.inventory.application.dto.ItemResponse;
import com.erp.inventory.application.service.ItemService;
import com.erp.inventory.domain.model.CostMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ItemService itemService;

    private ItemResponse buildItemResponse() {
        return new ItemResponse(1L, "SKU-001", "테스트품목", null, null, null,
                1L, "EA", "개", CostMethod.WEIGHTED_AVG,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, false, false, true);
    }

    @Test
    void findAll_returnsOkWithPage() throws Exception {
        given(itemService.findAll(isNull(), any())).willReturn(
                new PageResponse<>(List.of(buildItemResponse()), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/inventory/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].sku").value("SKU-001"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(itemService.create(any())).willReturn(buildItemResponse());

        ItemCreateRequest req = new ItemCreateRequest("SKU-001", "테스트품목", null, null, 1L,
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("SKU-001"));
    }

    @Test
    void create_duplicateSku_returns409() throws Exception {
        given(itemService.create(any())).willThrow(new ErpException(ErrorCode.ITEM_SKU_DUPLICATE));

        ItemCreateRequest req = new ItemCreateRequest("SKU-001", "테스트품목", null, null, 1L,
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
