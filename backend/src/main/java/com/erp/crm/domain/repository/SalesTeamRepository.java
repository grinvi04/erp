package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.SalesTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesTeamRepository extends JpaRepository<SalesTeam, Long> {

    List<SalesTeam> findAllByOrderByCodeAsc();

    Optional<SalesTeam> findByCode(String code);

    boolean existsByCode(String code);
}
