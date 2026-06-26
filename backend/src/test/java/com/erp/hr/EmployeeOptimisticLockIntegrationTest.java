package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.dto.EmployeeUpdateRequest;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 직원 갱신의 HTTP read-modify-write 낙관적 잠금 계약 검증 — 폼 로드 시 받은 version과
 * 현재 영속 version이 다르면(그 사이 타인이 수정) 거부해 lost update를 막는다.
 * GlobalExceptionHandler가 {@link ObjectOptimisticLockingFailureException} → 409로 매핑.
 */
@Transactional
class EmployeeOptimisticLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;

    private Long deptId;
    private Long posId;

    @BeforeEach
    void setUp() {
        deptId = departmentRepository.save(Department.createRoot("DEV", "개발팀")).getId();
        posId = positionRepository.save(Position.of("P001", "Engineer", 2)).getId();
        authenticate("hr-admin");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub).claim("sub", sub).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("hr:employee:write"),
                        new SimpleGrantedAuthority("hr:employee:read"))));
    }

    private EmployeeResponse createEmployee() {
        return employeeService.create(new EmployeeCreateRequest(
                "EMP-100", "김", "직원", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE,
                null, null, null, deptId, posId, null, LocalDate.of(2024, 1, 1),
                EmploymentType.REGULAR, "emp100@test.com", BigDecimal.valueOf(50000000), null));
    }

    private EmployeeUpdateRequest updateWithVersion(Long version) {
        return new EmployeeUpdateRequest(
                "이", "직원", null, null, null, null, null, null, version);
    }

    @Test
    void update_staleVersion_throwsOptimisticLockConflict() {
        EmployeeResponse created = createEmployee();
        Long staleVersion = created.version() + 1;

        assertThatThrownBy(() -> employeeService.update(created.id(), updateWithVersion(staleVersion)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void update_currentVersion_succeeds() {
        EmployeeResponse created = createEmployee();

        EmployeeResponse updated = employeeService.update(created.id(), updateWithVersion(created.version()));

        assertThat(updated.lastName()).isEqualTo("이");
    }
}
