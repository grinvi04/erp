package com.erp.hr.application.dto;

import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
    Long id,
    String employeeNo,
    String lastName,
    String firstName,
    String fullName,
    LocalDate dateOfBirth,
    PersonalInfo.Gender gender,
    String phone,
    String personalEmail,
    Long departmentId,
    String departmentName,
    Long positionId,
    String positionName,
    Long jobGradeId,
    String jobGradeName,
    LocalDate hireDate,
    LocalDate terminationDate,
    EmploymentType employmentType,
    EmployeeStatus status,
    BigDecimal baseSalary,
    String workEmail,
    Long managerId,
    String userId
) {
    public static EmployeeResponse from(Employee emp) {
        return new EmployeeResponse(
            emp.getId(),
            emp.getEmployeeNo(),
            emp.getPersonalInfo().getLastName(),
            emp.getPersonalInfo().getFirstName(),
            emp.getPersonalInfo().getFullName(),
            emp.getPersonalInfo().getDateOfBirth(),
            emp.getPersonalInfo().getGender(),
            emp.getPersonalInfo().getPhone(),
            emp.getPersonalInfo().getPersonalEmail(),
            emp.getDepartment().getId(),
            emp.getDepartment().getName(),
            emp.getPosition().getId(),
            emp.getPosition().getName(),
            emp.getJobGrade() != null ? emp.getJobGrade().getId() : null,
            emp.getJobGrade() != null ? emp.getJobGrade().getName() : null,
            emp.getHireDate(),
            emp.getTerminationDate(),
            emp.getEmploymentType(),
            emp.getStatus(),
            emp.getBaseSalary(),
            emp.getWorkEmail(),
            emp.getManager() != null ? emp.getManager().getId() : null,
            emp.getUserId()
        );
    }
}
