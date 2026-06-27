package com.erp.inventory.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.ItemCategoryCreateRequest;
import com.erp.inventory.application.dto.ItemCategoryResponse;
import com.erp.inventory.application.service.ItemCategoryService;
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

@WebMvcTest(ItemCategoryController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ItemCategoryControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private ItemCategoryService itemCategoryService;

  @Test
  void findAll_returnsOkWithList() throws Exception {
    given(itemCategoryService.findAll())
        .willReturn(List.of(new ItemCategoryResponse(1L, "ELEC", "전자", null, null, 0L)));

    mockMvc
        .perform(get("/api/inventory/item-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].code").value("ELEC"));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    given(itemCategoryService.create(any()))
        .willReturn(new ItemCategoryResponse(1L, "ELEC", "전자", null, null, 0L));

    mockMvc
        .perform(
            post("/api/inventory/item-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ItemCategoryCreateRequest("ELEC", "전자", null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.code").value("ELEC"));
  }

  @Test
  void delete_withChildren_returns409() throws Exception {
    willThrow(new ErpException(ErrorCode.ITEM_CATEGORY_HAS_CHILDREN))
        .given(itemCategoryService)
        .delete(1L);

    mockMvc.perform(delete("/api/inventory/item-categories/1")).andExpect(status().isConflict());
  }
}
