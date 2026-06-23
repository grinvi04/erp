package com.erp.inventory.application.service;

import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.StockResponse;
import com.erp.inventory.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ItemService itemService;
    private final WarehouseService warehouseService;

    public PageResponse<StockResponse> findByItem(Long itemId, Pageable pageable) {
        itemService.getOrThrow(itemId);
        return PageResponse.from(stockRepository.findByItem_Id(itemId, pageable).map(StockResponse::from));
    }

    public PageResponse<StockResponse> findByWarehouse(Long warehouseId, Pageable pageable) {
        warehouseService.getOrThrow(warehouseId);
        return PageResponse.from(stockRepository.findByWarehouseId(warehouseId, pageable).map(StockResponse::from));
    }
}
