package com.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.inventory.application.service.UomService;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 단위(UOM) 삭제 시 참조 품목이 있으면 거부되는지(카테고리 삭제 가드와 대칭) 실제 파생 쿼리(existsByUom_Id)를 통해 검증한다. */
@Transactional
class UomDeleteReferenceGuardIntegrationTest extends AbstractIntegrationTest {

  @Autowired private UomService uomService;
  @Autowired private ItemRepository itemRepository;
  @Autowired private ItemCategoryRepository itemCategoryRepository;
  @Autowired private UnitOfMeasureRepository uomRepository;

  @Test
  void delete_uomReferencedByItem_isRejected() {
    authenticate("tester", Permission.INVENTORY_WRITE);
    UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.of("UOM-REF", "Each"));
    ItemCategory cat = itemCategoryRepository.save(ItemCategory.of("CAT-REF", "분류", null));
    itemRepository.saveAndFlush(
        Item.of(
            "SKU-REF",
            "품목",
            null,
            cat,
            uom,
            CostMethod.WEIGHTED_AVG,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.valueOf(1000),
            false,
            false));

    assertThatThrownBy(() -> uomService.delete(uom.getId()))
        .isInstanceOf(ErpException.class)
        .extracting(e -> ((ErpException) e).getErrorCode())
        .isEqualTo(ErrorCode.UOM_IN_USE);

    assertThat(uomRepository.findById(uom.getId()).orElseThrow().getDeletedAt()).isNull();
  }

  @Test
  void delete_unreferencedUom_softDeletes() {
    authenticate("tester", Permission.INVENTORY_WRITE);
    UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.of("UOM-FREE", "Each"));

    uomService.delete(uom.getId());

    assertThat(uomRepository.findById(uom.getId()).orElseThrow().getDeletedAt()).isNotNull();
  }
}
