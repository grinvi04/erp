package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
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
class EmployeeSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    @BeforeEach
    void setUp() {
        Position pos = positionRepository.save(Position.of("P1", "Engineer", 2));
        Department dept = departmentRepository.save(Department.createRoot("A", "본부A"));
        save("EMP-1001", "김", "철수", dept, pos);
        save("EMP-2002", "이", "영희", dept, pos);
        // ALL 스코프로 인증 — 검색 동작만 검증(데이터 스코프 필터는 별도 테스트가 커버)
        authenticate("user-search");
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void save(String empNo, String lastName, String firstName, Department dept, Position pos) {
        PersonalInfo info = new PersonalInfo(lastName, firstName, LocalDate.of(1990, 1, 1),
                PersonalInfo.Gender.MALE, null, null, null);
        Employee e = Employee.create(empNo, info, dept, pos, null,
                LocalDate.of(2020, 1, 1), EmploymentType.REGULAR, empNo + "@test.com",
                BigDecimal.valueOf(50000000));
        employeeRepository.save(e);
    }

    private void authenticate(String sub) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("hr:employee:read"))));
        accessProfileRepository.save(UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, null));
    }

    private List<String> searchEmpNos(String keyword) {
        return employeeService.findAll(null, null, keyword, PageRequest.of(0, 50))
                .getContent().stream().map(EmployeeResponse::employeeNo).toList();
    }

    @Test
    void nullKeyword_returnsAll() {
        assertThat(searchEmpNos(null)).containsExactlyInAnyOrder("EMP-1001", "EMP-2002");
    }

    @Test
    void blankKeyword_normalizedToNull_returnsAll() {
        assertThat(searchEmpNos("   ")).containsExactlyInAnyOrder("EMP-1001", "EMP-2002");
    }

    @Test
    void keyword_matchesByFirstName() {
        assertThat(searchEmpNos("철수")).containsExactly("EMP-1001");
    }

    @Test
    void keyword_matchesByEmployeeNo_caseInsensitive() {
        // 소문자 "emp-2002" → 사번 "EMP-2002" 매칭(대소문자 무시)
        assertThat(searchEmpNos("emp-2002")).containsExactly("EMP-2002");
    }

    @Test
    void keyword_noMatch_returnsEmpty() {
        assertThat(searchEmpNos("없는이름")).isEmpty();
    }
}
