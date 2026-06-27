package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.inventory.application.dto.ItemCategoryCreateRequest;
import com.erp.inventory.application.dto.ItemCategoryResponse;
import com.erp.inventory.application.dto.ItemCategoryUpdateRequest;
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
  private final PermissionChecker permissionChecker;

  public List<ItemCategoryResponse> findAll() {
    permissionChecker.require(Permission.INVENTORY_READ);
    return categoryRepository.findAll().stream().map(ItemCategoryResponse::from).toList();
  }

  public List<ItemCategoryResponse> findRoots() {
    permissionChecker.require(Permission.INVENTORY_READ);
    return categoryRepository.findByParentIsNull().stream()
        .map(ItemCategoryResponse::from)
        .toList();
  }

  public List<ItemCategoryResponse> findChildren(Long parentId) {
    permissionChecker.require(Permission.INVENTORY_READ);
    getOrThrow(parentId);
    return categoryRepository.findByParent_Id(parentId).stream()
        .map(ItemCategoryResponse::from)
        .toList();
  }

  public ItemCategoryResponse findById(Long id) {
    permissionChecker.require(Permission.INVENTORY_READ);
    return ItemCategoryResponse.from(getOrThrow(id));
  }

  @Transactional
  public ItemCategoryResponse create(ItemCategoryCreateRequest req) {
    permissionChecker.require(Permission.INVENTORY_WRITE);
    if (categoryRepository.existsByCode(req.code().toUpperCase())) {
      throw new ErpException(ErrorCode.ITEM_CATEGORY_CODE_DUPLICATE);
    }
    ItemCategory parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
    return ItemCategoryResponse.from(
        categoryRepository.save(ItemCategory.of(req.code(), req.name(), parent)));
  }

  @Transactional
  public ItemCategoryResponse update(Long id, ItemCategoryUpdateRequest req) {
    permissionChecker.require(Permission.INVENTORY_WRITE);
    ItemCategory cat = getOrThrow(id);
    cat.checkVersion(req.version());
    ItemCategory parent = req.parentId() != null ? getOrThrow(req.parentId()) : null;
    cat.update(req.name(), parent);
    return ItemCategoryResponse.from(cat);
  }

  @Transactional
  public void delete(Long id) {
    permissionChecker.require(Permission.INVENTORY_WRITE);
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
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.ITEM_CATEGORY_NOT_FOUND));
  }
}
