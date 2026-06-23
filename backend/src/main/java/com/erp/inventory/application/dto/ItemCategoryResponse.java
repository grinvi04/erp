package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.ItemCategory;

public record ItemCategoryResponse(
        Long id,
        String code,
        String name,
        Long parentId,
        String parentName
) {
    public static ItemCategoryResponse from(ItemCategory cat) {
        Long parentId = cat.getParent() != null ? cat.getParent().getId() : null;
        String parentName = cat.getParent() != null ? cat.getParent().getName() : null;
        return new ItemCategoryResponse(cat.getId(), cat.getCode(), cat.getName(), parentId, parentName);
    }
}
