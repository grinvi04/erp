package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.ItemCreateRequest;
import com.erp.inventory.application.dto.ItemResponse;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemCategoryService itemCategoryService;
    @Mock private UomService uomService;
    @InjectMocks private ItemService itemService;

    private UnitOfMeasure buildUom() {
        return UnitOfMeasure.of("EA", "개");
    }

    private Item buildItem() {
        return Item.of("SKU-001", "테스트품목", null, null, buildUom(),
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
    }

    @Test
    void findAll_noCategoryFilter_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(itemRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(buildItem()), pageable, 1));

        PageResponse<ItemResponse> result = itemService.findAll(null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).sku()).isEqualTo("SKU-001");
    }

    @Test
    void create_newSku_savesAndReturns() {
        UnitOfMeasure uom = buildUom();
        Item item = buildItem();
        given(itemRepository.existsBySku("SKU-001")).willReturn(false);
        given(uomService.getEntityOrThrow(1L)).willReturn(uom);
        given(itemRepository.save(any())).willReturn(item);

        ItemCreateRequest req = new ItemCreateRequest("SKU-001", "테스트품목", null, null, 1L,
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
        ItemResponse result = itemService.create(req);

        assertThat(result.sku()).isEqualTo("SKU-001");
        assertThat(result.active()).isTrue();
    }

    @Test
    void create_duplicateSku_throwsItemSkuDuplicate() {
        given(itemRepository.existsBySku("SKU-001")).willReturn(true);

        ItemCreateRequest req = new ItemCreateRequest("SKU-001", "테스트품목", null, null, 1L,
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
        ErpException ex = assertThrows(ErpException.class, () -> itemService.create(req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_SKU_DUPLICATE);
    }

    @Test
    void deactivate_existingItem_setsInactive() {
        Item item = buildItem();
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        itemService.deactivate(1L);

        assertThat(item.isActive()).isFalse();
    }

    @Test
    void findById_notFound_throwsItemNotFound() {
        given(itemRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> itemService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }
}
