package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EmployeeDataScopeIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;

    private Department deptA;
    private Employee empA;

    @BeforeEach
    void setUp() {
        Position pos = positionRepository.save(Position.of("P1", "Engineer", 2));
        deptA = departmentRepository.save(Department.createRoot("A", "본부A"));
        Department deptB = departmentRepository.save(Department.createChild("B", "팀B", deptA, 1));
        Department deptC = departmentRepository.save(Department.createRoot("C", "본부C"));

        empA = save("EMP-A", "user-a", deptA, pos);
        save("EMP-B", "user-b", deptB, pos);
        save("EMP-C", "user-c", deptC, pos);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private Employee save(String empNo, String userId, Department dept, Position pos) {
        PersonalInfo info = new PersonalInfo("성", empNo, LocalDate.of(1990, 1, 1),
                PersonalInfo.Gender.MALE, null, null, null);
        Employee e = Employee.create(empNo, info, dept, pos, null,
                LocalDate.of(2020, 1, 1), EmploymentType.REGULAR, empNo + "@test.com",
                BigDecimal.valueOf(50000000));
        e.linkUserAccount(userId);
        return employeeRepository.save(e);
    }

    private void authenticate(String sub, String dataScope, Long departmentId) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject(sub).claim("sub", sub);
        if (dataScope != null) {
            b.claim("data_scope", dataScope);
        }
        if (departmentId != null) {
            b.claim("department_id", departmentId);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(b.build(),
                List.of(new SimpleGrantedAuthority("hr:employee:read"))));
    }

    private List<String> findAllEmpNos() {
        Page<EmployeeResponse> page = employeeService.findAll(null, null, PageRequest.of(0, 50));
        return page.getContent().stream().map(EmployeeResponse::employeeNo).toList();
    }

    @Test
    void departmentScope_returnsOwnDeptAndDescendants_notSiblings() {
        authenticate("user-a", "DEPARTMENT", deptA.getId());

        // 본부A + 하위 팀B는 보이고, 별개 본부C는 제외
        assertThat(findAllEmpNos()).containsExactlyInAnyOrder("EMP-A", "EMP-B");
    }

    @Test
    void selfScope_returnsOnlyOwnRecord() {
        authenticate("user-a", "SELF", null);

        assertThat(findAllEmpNos()).containsExactly("EMP-A");
    }

    @Test
    void allScope_returnsEveryone() {
        authenticate("user-a", "ALL", null);

        assertThat(findAllEmpNos()).containsExactlyInAnyOrder("EMP-A", "EMP-B", "EMP-C");
    }

    @Test
    void findById_outOfDepartmentScope_throwsForbidden() {
        // user-a는 본부A 스코프 — 본부C의 직원은 단건 조회도 차단
        Employee empC = employeeRepository.findAll().stream()
                .filter(e -> e.getEmployeeNo().equals("EMP-C")).findFirst().orElseThrow();
        authenticate("user-a", "DEPARTMENT", deptA.getId());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.erp.common.exception.ErpException.class,
                () -> employeeService.findById(empC.getId()));
    }
}
