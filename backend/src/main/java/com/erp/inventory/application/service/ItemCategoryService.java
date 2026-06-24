package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.dto.ItemCategoryCreateRequest;
import com.erp.inventory.application.dto.ItemCategoryResponse;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public List<ItemCategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(ItemCategoryResponse::from).toList();
    }

    public List<ItemCategoryResponse> findRoots() {
        return categoryRepository.findByParentIsNull().stream().map(ItemCategoryResponse::from).toList();
    }

    public List<ItemCategoryResponse> findChildren(Long parentId) {
        getOrThrow(parentId);
        return categoryRepository.findByParent_Id(parentId).stream().map(ItemCategoryResponse::from).toList();
    }

    public ItemCategoryResponse findById(Long id) {
        return ItemCategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public ItemCategoryResponse create(ItemCategoryCreateRequest req) {
        if (categoryRepository.existsByCode(req.code().toUpperCase())) {
            throw new ErpException(ErrorCode.ITEM_CATEGORY_CODE_DUPLICATE);
        }
        ItemCategory parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
        return ItemCategoryResponse.from(
                categoryRepository.save(ItemCategory.of(req.code(), req.name(), parent)));
    }

    @Transactional
    public ItemCategoryResponse update(Long id, ItemCategoryCreateRequest req) {
        ItemCategory cat = getOrThrow(id);
        ItemCategory parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
        cat.update(req.name(), parent);
        return ItemCategoryResponse.from(cat);
    }

    @Transactional
    public void delete(Long id) {
        ItemCategory cat = getOrThrow(id);
        if (categoryRepository.existsByParent_Id(id)) {
            throw new ErpException(ErrorCode.ITEM_CATEGORY_HAS_CHILDREN);
        }
        if (itemRepository.existsByCategory_Id(id)) {
            throw new ErpException(ErrorCode.ITEM_CATEGORY_HAS_ITEMS);
        }
        cat.softDelete();
    }

    public ItemCategory getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.ITEM_CATEGORY_NOT_FOUND));
    }
}
