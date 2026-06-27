package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.TenantBaseCurrency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantBaseCurrencyRepository extends JpaRepository<TenantBaseCurrency, Long> {
  // tenant_id는 @TenantId로 자동 필터되므로(테넌트당 1행) 현재 테넌트의 설정을 단건 조회한다.
  Optional<TenantBaseCurrency> findFirstByOrderByIdAsc();
}
