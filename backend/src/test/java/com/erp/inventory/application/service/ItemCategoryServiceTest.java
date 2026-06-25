package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.ItemCategoryCreateRequest;
import com.erp.inventory.application.dto.ItemCategoryResponse;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ItemCategoryServiceTest {

    @Mock private ItemCategoryRepository categoryRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private ItemCategoryService itemCategoryService;

    @Test
    void findRoots_returnsOnlyRootCategories() {
        ItemCategory root = ItemCategory.of("ELEC", "전자", null);
        given(categoryRepository.findByParentIsNull()).willReturn(List.of(root));

        List<ItemCategoryResponse> result = itemCategoryService.findRoots();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("ELEC");
    }

    @Test
    void create_newCode_savesAndReturns() {
        ItemCategory cat = ItemCategory.of("ELEC", "전자", null);
        given(categoryRepository.existsByCode("ELEC")).willReturn(false);
        given(categoryRepository.save(any())).willReturn(cat);

        ItemCategoryResponse result = itemCategoryService.create(
                new ItemCategoryCreateRequest("ELEC", "전자", null));

        assertThat(result.code()).isEqualTo("ELEC");
        assertThat(result.parentId()).isNull();
    }

    @Test
    void create_duplicateCode_throwsItemCategoryCodeDuplicate() {
        given(categoryRepository.existsByCode("ELEC")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class,
                () -> itemCategoryService.create(new ItemCategoryCreateRequest("ELEC", "전자", null)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_CATEGORY_CODE_DUPLICATE);
    }

    @Test
    void delete_withChildren_throwsItemCategoryHasChildren() {
        ItemCategory cat = ItemCategory.of("ELEC", "전자", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(cat));
        given(categoryRepository.existsByParent_Id(1L)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () -> itemCategoryService.delete(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_CATEGORY_HAS_CHILDREN);
    }

    @Test
    void delete_withItems_throwsItemCategoryHasItems() {
        ItemCategory cat = ItemCategory.of("ELEC", "전자", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(cat));
        given(categoryRepository.existsByParent_Id(1L)).willReturn(false);
        given(itemRepository.existsByCategory_Id(1L)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () -> itemCategoryService.delete(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_CATEGORY_HAS_ITEMS);
    }

    @Test
    void findChildren_parentNotFound_throwsItemCategoryNotFound() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> itemCategoryService.findChildren(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_CATEGORY_NOT_FOUND);
    }
}
