package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.ItemCreateRequest;
import com.erp.inventory.application.dto.ItemResponse;
import com.erp.inventory.application.dto.ItemUpdateRequest;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryService itemCategoryService;
    private final UomService uomService;

    public PageResponse<ItemResponse> findAll(Long categoryId, Pageable pageable) {
        Page<Item> page = categoryId != null
                ? itemRepository.findByCategory_Id(categoryId, pageable)
                : itemRepository.findAll(pageable);
        return PageResponse.from(page.map(ItemResponse::from));
    }

    public ItemResponse findById(Long id) {
        return ItemResponse.from(getOrThrow(id));
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest req) {
        if (itemRepository.existsBySku(req.sku().toUpperCase())) {
            throw new ErpException(ErrorCode.ITEM_SKU_DUPLICATE);
        }
        ItemCategory category = req.categoryId() != null
                ? itemCategoryService.getOrThrow(req.categoryId()) : null;
        UnitOfMeasure uom = uomService.getEntityOrThrow(req.uomId());
        Item item = Item.of(req.sku(), req.name(), req.description(), category, uom,
                req.costMethod(), req.standardCost(), req.reorderPoint(), req.reorderQty(),
                req.minStock(), req.maxStock(), req.lotTracked(), req.serialTracked());
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest req) {
        Item item = getOrThrow(id);
        ItemCategory category = req.categoryId() != null
                ? itemCategoryService.getOrThrow(req.categoryId()) : null;
        UnitOfMeasure uom = uomService.getEntityOrThrow(req.uomId());
        item.update(req.name(), req.description(), category, uom,
                req.costMethod(), req.standardCost(), req.reorderPoint(), req.reorderQty(),
                req.minStock(), req.maxStock(), req.lotTracked(), req.serialTracked());
        return ItemResponse.from(item);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).deactivate();
    }

    public Item getOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.ITEM_NOT_FOUND));
    }
}
