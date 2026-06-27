package com.erp.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.erp.inventory.application.dto.InventorySummaryResponse;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.MovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventorySummaryServiceTest {

  @Mock private ItemRepository itemRepository;
  @Mock private MovementRepository movementRepository;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @InjectMocks private InventorySummaryService inventorySummaryService;

  @Test
  void getSummary_aggregatesItemAndMovementCounts() {
    given(itemRepository.countByActiveTrue()).willReturn(120L);
    given(itemRepository.countLowStockItems()).willReturn(8L);
    given(movementRepository.countByStatus(MovementStatus.DRAFT)).willReturn(4L);

    InventorySummaryResponse result = inventorySummaryService.getSummary();

    assertThat(result.activeItems()).isEqualTo(120L);
    assertThat(result.lowStockItems()).isEqualTo(8L);
    assertThat(result.draftMovements()).isEqualTo(4L);
  }
}
