package com.erp.inventory.application.dto;

public record InventorySummaryResponse(long activeItems, long lowStockItems, long draftMovements) {}
