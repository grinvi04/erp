package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.CompanyProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
  // tenant_id는 @TenantId로 자동 필터되므로(테넌트당 1행) 현재 테넌트의 회사정보를 단건 조회한다.
  Optional<CompanyProfile> findFirstByOrderByIdAsc();
}
