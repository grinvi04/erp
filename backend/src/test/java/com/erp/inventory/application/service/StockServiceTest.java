package com.erp.inventory.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.StockResponse;
import com.erp.inventory.domain.model.Item;
import com.erp.inventory.domain.model.Location;
import com.erp.inventory.domain.model.LocationType;
import com.erp.inventory.domain.model.Stock;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Warehouse;
import com.erp.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private ItemService itemService;
    @Mock private WarehouseService warehouseService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private StockService stockService;

    private Item buildItem() {
        return Item.of("SKU-001", "테스트품목", null, null, UnitOfMeasure.of("EA", "개"),
                CostMethod.WEIGHTED_AVG, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
    }

    private Stock buildStock(Item item) {
        Warehouse wh = Warehouse.of("WH-001", "본창고", "서울");
        Location loc = Location.of(wh, "A-01", "구역A", null, LocationType.ZONE);
        Stock s = Stock.create(item, loc, null, null, BigDecimal.ONE);
        s.increase(BigDecimal.TEN, BigDecimal.ONE);
        return s;
    }

    @Test
    void findByItem_existingItem_returnsPage() {
        Item item = buildItem();
        Stock stock = buildStock(item);
        PageRequest pageable = PageRequest.of(0, 10);
        given(itemService.getOrThrow(1L)).willReturn(item);
        given(stockRepository.findByItem_Id(eq(1L), any())).willReturn(
                new PageImpl<>(List.of(stock), pageable, 1));

        PageResponse<StockResponse> result = stockService.findByItem(1L, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).itemSku()).isEqualTo("SKU-001");
        assertThat(result.content().get(0).qtyOnHand()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void findByItem_itemNotFound_throwsItemNotFound() {
        given(itemService.getOrThrow(99L)).willThrow(new ErpException(ErrorCode.ITEM_NOT_FOUND));

        ErpException ex = assertThrows(ErpException.class,
                () -> stockService.findByItem(99L, PageRequest.of(0, 10)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void findByWarehouse_existingWarehouse_returnsPage() {
        Item item = buildItem();
        Stock stock = buildStock(item);
        PageRequest pageable = PageRequest.of(0, 10);
        given(warehouseService.getOrThrow(1L)).willReturn(Warehouse.of("WH-001", "본창고", "서울"));
        given(stockRepository.findByWarehouseId(eq(1L), any())).willReturn(
                new PageImpl<>(List.of(stock), pageable, 1));

        PageResponse<StockResponse> result = stockService.findByWarehouse(1L, pageable);

        assertThat(result.content()).hasSize(1);
    }
}
