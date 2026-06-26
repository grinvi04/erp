package com.erp.inventory.domain.repository;

import com.erp.inventory.domain.model.MovementType;
import java.math.BigDecimal;

public interface MonthlyMovementRow {
    MovementType getMovementType();
    Integer getMonth();
    long getCount();
    BigDecimal getTotalQty();
}
