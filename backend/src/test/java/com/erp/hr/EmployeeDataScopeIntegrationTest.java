package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.application.service.LeaveRequestService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
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
    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeavePolicyRepository leavePolicyRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    private Department deptA;
    private Employee empA;
    private Employee empC;

    @BeforeEach
    void setUp() {
        Position pos = positionRepository.save(Position.of("P1", "Engineer", 2));
        deptA = departmentRepository.save(Department.createRoot("A", "본부A"));
        Department deptB = departmentRepository.save(Department.createChild("B", "팀B", deptA, 1));
        Department deptC = departmentRepository.save(Department.createRoot("C", "본부C"));

        empA = save("EMP-A", "user-a", deptA, pos);
        save("EMP-B", "user-b", deptB, pos);
        empC = save("EMP-C", "user-c", deptC, pos);
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
        // 신원(sub·tenant_id)은 JWT, 데이터 스코프는 DB 접근 프로파일에서 해석(전면 DB 전환).
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("hr:employee:read"),
                        new SimpleGrantedAuthority("hr:leave:read"))));
        DataScope scope = dataScope != null ? DataScope.valueOf(dataScope) : DataScope.ALL;
        accessProfileRepository.save(UserAccessProfile.of(TEST_TENANT_ID, sub, scope, departmentId, null));
    }

    private List<String> findAllEmpNos() {
        Page<EmployeeResponse> page = employeeService.findAll(null, null, null, PageRequest.of(0, 50));
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
    void leaveRequestFindAll_departmentScope_excludesOutOfScopeEmployees() {
        // 형제 경로 우회 차단 검증: 휴가신청 목록도 데이터 스코프로 필터(범위 밖 직원 제외)
        LeavePolicy policy = leavePolicyRepository.save(
                LeavePolicy.of("ANN", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1));
        leaveRequestRepository.save(LeaveRequest.create(empA, policy,
                LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 2), BigDecimal.valueOf(2), "A"));
        leaveRequestRepository.save(LeaveRequest.create(empC, policy,
                LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 2), BigDecimal.valueOf(2), "C"));

        authenticate("user-a", "DEPARTMENT", deptA.getId());

        List<Long> empIds = leaveRequestService.findAll(PageRequest.of(0, 50))
                .getContent().stream().map(LeaveRequestResponse::employeeId).toList();
        // 본부A의 empA만, 본부C의 empC는 제외
        assertThat(empIds).containsExactly(empA.getId());
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
