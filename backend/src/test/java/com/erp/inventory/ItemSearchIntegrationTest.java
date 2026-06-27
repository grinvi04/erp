package com.erp.inventory;

import com.erp.common.AbstractIntegrationTest;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.ItemCategory;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ItemSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;

    private final PageRequest pageable = PageRequest.of(0, 20);
    private Long electronicsCategoryId;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = uomRepository.save(UnitOfMeasure.of("EA", "Each"));
        ItemCategory electronics = itemCategoryRepository.save(
                ItemCategory.of("CAT-ELEC", "Electronics", null));
        ItemCategory tools = itemCategoryRepository.save(
                ItemCategory.of("CAT-TOOL", "Tools", null));
        electronicsCategoryId = electronics.getId();

        save("WIDGET-1", "Blue Widget", electronics, uom);
        save("GADGET-2", "Red Gadget", electronics, uom);
        save("HAMMER-3", "Steel Widget Hammer", tools, uom);
    }

    private void save(String sku, String name, ItemCategory category, UnitOfMeasure uom) {
        itemRepository.save(Item.of(sku, name, null, category, uom,
                CostMethod.WEIGHTED_AVG, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(1000), false, false));
    }

    private List<String> searchSkus(String keyword, Long categoryId) {
        return itemRepository.search(keyword, categoryId, pageable).getContent().stream()
                .map(Item::getSku).toList();
    }

    @Test
    void nullKeyword_nullCategory_returnsAll() {
        assertThat(searchSkus(null, null))
                .containsExactlyInAnyOrder("WIDGET-1", "GADGET-2", "HAMMER-3");
    }

    @Test
    void keyword_matchesBySkuAndName_caseInsensitive() {
        // 소문자 "widget" → SKU "WIDGET-1" + 이름 "Steel Widget Hammer"(HAMMER-3) 둘 다 매칭
        assertThat(searchSkus("widget", null))
                .containsExactlyInAnyOrder("WIDGET-1", "HAMMER-3");
    }

    @Test
    void keyword_matchesByNamePartial() {
        assertThat(searchSkus("gadget", null)).containsExactly("GADGET-2");
    }

    @Test
    void categoryFilter_withoutKeyword_returnsCategoryItems() {
        assertThat(searchSkus(null, electronicsCategoryId))
                .containsExactlyInAnyOrder("WIDGET-1", "GADGET-2");
    }

    @Test
    void keywordAndCategory_combineAsAnd() {
        // "widget" + Electronics 카테고리 → WIDGET-1만(HAMMER-3은 Tools 카테고리라 제외)
        assertThat(searchSkus("widget", electronicsCategoryId)).containsExactly("WIDGET-1");
    }

    @Test
    void keyword_noMatch_returnsEmpty() {
        assertThat(searchSkus("nonexistent", null)).isEmpty();
    }
}
