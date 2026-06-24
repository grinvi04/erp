package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.InventorySummaryResponse;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventorySummaryService {

    private final ItemRepository itemRepository;
    private final MovementRepository movementRepository;

    public InventorySummaryResponse getSummary() {
        return new InventorySummaryResponse(
                itemRepository.countByActiveTrue(),
                itemRepository.countLowStockItems(),
                movementRepository.countByStatus(MovementStatus.DRAFT));
    }
}
