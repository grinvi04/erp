package com.erp.inventory.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.StockResponse;
import com.erp.inventory.application.service.StockService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class StockControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StockService stockService;

    private StockResponse buildStockResponse() {
        return new StockResponse(1L, 1L, "SKU-001", "테스트품목",
                1L, "A-01", "구역A", 1L, "본창고",
                null, null, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ONE);
    }

    @Test
    void findByItem_returnsOkWithPage() throws Exception {
        PageResponse<StockResponse> page = new PageResponse<>(
                List.of(buildStockResponse()), 0, 10, 1, 1, true, true);
        given(stockService.findByItem(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/api/inventory/stocks/by-item").param("itemId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemSku").value("SKU-001"));
    }

    @Test
    void findByWarehouse_returnsOkWithPage() throws Exception {
        PageResponse<StockResponse> page = new PageResponse<>(
                List.of(buildStockResponse()), 0, 10, 1, 1, true, true);
        given(stockService.findByWarehouse(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/api/inventory/stocks/by-warehouse").param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].warehouseName").value("본창고"));
    }
}
