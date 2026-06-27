package com.erp.hr.application.dto;

import com.erp.hr.domain.model.Contract;
import com.erp.hr.domain.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
    Long id,
    Long employeeId,
    ContractType contractType,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal baseSalary,
    Long positionId,
    String positionName,
    Long jobGradeId,
    String jobGradeName,
    String note) {
  public static ContractResponse from(Contract contract) {
    return new ContractResponse(
        contract.getId(),
        contract.getEmployee().getId(),
        contract.getContractType(),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getBaseSalary(),
        contract.getPosition().getId(),
        contract.getPosition().getName(),
        contract.getJobGrade() != null ? contract.getJobGrade().getId() : null,
        contract.getJobGrade() != null ? contract.getJobGrade().getName() : null,
        contract.getNote());
  }
}
