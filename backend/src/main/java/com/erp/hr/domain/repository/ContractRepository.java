package com.erp.hr.domain.repository;

import com.erp.hr.domain.model.Contract;
import com.erp.hr.domain.model.ContractType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
  List<Contract> findByEmployeeId(Long employeeId);

  Optional<Contract> findTopByEmployeeIdOrderByStartDateDesc(Long employeeId);

  List<Contract> findByEmployeeIdAndContractType(Long employeeId, ContractType contractType);
}
