package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.hr.application.dto.DepartmentHeadcountResponse;
import com.erp.hr.application.dto.EmployeeStatusCountResponse;
import com.erp.hr.application.dto.EmploymentTypeCountResponse;
import com.erp.hr.application.dto.LeaveTypeStatResponse;
import com.erp.hr.application.dto.MonthlyHiresTerminationsResponse;
import com.erp.hr.application.dto.PositionHeadcountResponse;
import com.erp.hr.application.service.HrAnalyticsService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.model.LeavePolicy.LeaveType;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import com.erp.hr.domain.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class HrAnalyticsIntegrationTest extends AbstractIntegrationTest {

    @Autowired private HrAnalyticsService hrAnalyticsService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeavePolicyRepository leavePolicyRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;

    private Department deptA;
    private Department deptB;
    private Department deptC;
    private Department deptEmpty;
    private Position posEng;
    private Position posMgr;
    private Position posUnused;
    private Employee empA;
    private Employee empC;

    @BeforeEach
    void setUpData() {
        posMgr = positionRepository.save(Position.of("MGR", "Manager", 1));
        posEng = positionRepository.save(Position.of("ENG", "Engineer", 2));
        posUnused = positionRepository.save(Position.of("UNU", "Unused", 3));

        deptA = departmentRepository.save(Department.createRoot("A", "본부A"));
        deptB = departmentRepository.save(Department.createChild("B", "팀B", deptA, 1));
        deptEmpty = departmentRepository.save(Department.createChild("E", "빈팀", deptA, 2));
        deptC = departmentRepository.save(Department.createRoot("C", "본부C"));

        empA = save("EMP-A", "user-a", deptA, posEng, EmploymentType.REGULAR, LocalDate.of(2026, 1, 10), false);
        save("EMP-B", "user-b", deptB, posMgr, EmploymentType.CONTRACT, LocalDate.of(2026, 3, 5), false);
        empC = save("EMP-C", "user-c", deptC, posEng, EmploymentType.REGULAR, LocalDate.of(2026, 1, 20), false);
        // empD: deptA, REGULAR, hired 2025, terminated 2026-06-15 — TERMINATED 상태/퇴사월 검증용
        save("EMP-D", "user-d", deptA, posEng, EmploymentType.REGULAR, LocalDate.of(2025, 1, 1), true);
    }

    private Employee save(String empNo, String userId, Department dept, Position pos,
                          EmploymentType type, LocalDate hireDate, boolean terminated) {
        PersonalInfo info = new PersonalInfo("성", empNo, LocalDate.of(1990, 1, 1),
                PersonalInfo.Gender.MALE, null, null, null);
        Employee e = Employee.create(empNo, info, dept, pos, null, hireDate, type,
                empNo + "@test.com", BigDecimal.valueOf(50000000));
        e.linkUserAccount(userId);
        if (terminated) {
            e.terminate(LocalDate.of(2026, 6, 15));
        }
        return employeeRepository.save(e);
    }

    private void authenticate(List<String> permissions, DataScope scope, Long deptId) {
        String sub = "user-a";
        authenticate(sub, permissions.toArray(new String[0]));
        accessProfileRepository.save(UserAccessProfile.of(TEST_TENANT_ID, sub, scope, deptId, null));
    }

    private void authenticateAll() {
        authenticate(List.of("hr:employee:read", "hr:leave:read"), DataScope.ALL, null);
    }

    private long count(List<EmployeeStatusCountResponse> rows, EmployeeStatus status) {
        return rows.stream().filter(r -> r.status() == status).findFirst().orElseThrow().count();
    }

    // AC-1
    @Test
    void statusDistribution_countsByStatus_allEnumValuesPresent() {
        authenticateAll();
        List<EmployeeStatusCountResponse> rows = hrAnalyticsService.getStatusDistribution();
        assertThat(rows).extracting(EmployeeStatusCountResponse::status)
                .containsExactly(EmployeeStatus.values());
        assertThat(count(rows, EmployeeStatus.ACTIVE)).isEqualTo(3L);
        assertThat(count(rows, EmployeeStatus.TERMINATED)).isEqualTo(1L);
        assertThat(count(rows, EmployeeStatus.ON_LEAVE)).isEqualTo(0L);
        assertThat(count(rows, EmployeeStatus.SUSPENDED)).isEqualTo(0L);
    }

    // AC-2
    @Test
    void byDepartment_countsActive_preservesEmptyDepartment() {
        authenticateAll();
        List<DepartmentHeadcountResponse> rows = hrAnalyticsService.getHeadcountByDepartment();
        assertThat(deptCount(rows, deptA.getId())).isEqualTo(1L); // empA ACTIVE (empD terminated 제외)
        assertThat(deptCount(rows, deptB.getId())).isEqualTo(1L);
        assertThat(deptCount(rows, deptC.getId())).isEqualTo(1L);
        assertThat(deptCount(rows, deptEmpty.getId())).isEqualTo(0L); // 빈 부서 보존
    }

    // AC-3
    @Test
    void byPosition_countsActive_preservesEmptyPosition_orderedByLevel() {
        authenticateAll();
        List<PositionHeadcountResponse> rows = hrAnalyticsService.getHeadcountByPosition();
        assertThat(rows).extracting(PositionHeadcountResponse::positionId)
                .containsExactly(posMgr.getId(), posEng.getId(), posUnused.getId()); // level_order 순
        assertThat(posCount(rows, posEng.getId())).isEqualTo(2L); // empA, empC ACTIVE (empD terminated 제외)
        assertThat(posCount(rows, posMgr.getId())).isEqualTo(1L);
        assertThat(posCount(rows, posUnused.getId())).isEqualTo(0L); // 빈 직위 보존
    }

    // AC-4
    @Test
    void byEmploymentType_countsAll_allEnumValuesPresent() {
        authenticateAll();
        List<EmploymentTypeCountResponse> rows = hrAnalyticsService.getEmploymentTypeDistribution();
        assertThat(rows).extracting(EmploymentTypeCountResponse::employmentType)
                .containsExactly(EmploymentType.values());
        assertThat(typeCount(rows, EmploymentType.REGULAR)).isEqualTo(3L); // empA, empC, empD
        assertThat(typeCount(rows, EmploymentType.CONTRACT)).isEqualTo(1L); // empB
        assertThat(typeCount(rows, EmploymentType.INTERN)).isEqualTo(0L);
    }

    // AC-5
    @Test
    void hiresTerminations_fillsAll12Months_twoSeries() {
        authenticateAll();
        List<MonthlyHiresTerminationsResponse> rows = hrAnalyticsService.getHiresTerminations(2026);
        assertThat(rows).hasSize(12);
        assertThat(rows.get(0).month()).isEqualTo(1);
        assertThat(rows.get(0).hires()).isEqualTo(2L);        // empA, empC 2026-01
        assertThat(rows.get(2).hires()).isEqualTo(1L);        // empB 2026-03
        assertThat(rows.get(5).terminations()).isEqualTo(1L); // empD 2026-06
        // empD hired 2025 → 2026 시리즈 입사에 미포함
        long totalHires = rows.stream().mapToLong(MonthlyHiresTerminationsResponse::hires).sum();
        assertThat(totalHires).isEqualTo(3L);
        long totalTerms = rows.stream().mapToLong(MonthlyHiresTerminationsResponse::terminations).sum();
        assertThat(totalTerms).isEqualTo(1L);
    }

    // AC-6
    @Test
    void leavesByType_countsApprovedOnly_withDaySum() {
        seedLeaves();
        authenticateAll();
        List<LeaveTypeStatResponse> rows = hrAnalyticsService.getLeavesByType();
        assertThat(rows).extracting(LeaveTypeStatResponse::leaveType)
                .containsExactly(LeaveType.values());
        LeaveTypeStatResponse annual = leave(rows, LeaveType.ANNUAL);
        assertThat(annual.count()).isEqualTo(2L);
        assertThat(annual.totalDays()).isEqualByComparingTo(BigDecimal.valueOf(5));
        LeaveTypeStatResponse sick = leave(rows, LeaveType.SICK);
        assertThat(sick.count()).isEqualTo(1L); // PENDING 1건 제외
        assertThat(sick.totalDays()).isEqualByComparingTo(BigDecimal.valueOf(1));
        assertThat(leave(rows, LeaveType.UNPAID).count()).isEqualTo(0L);
    }

    // AC-7 — DEPARTMENT 스코프는 자기 부서+하위만 집계, 다른 부서(deptC) 비노출
    @Test
    void departmentScope_excludesOtherDepartments() {
        seedLeaves();
        authenticate(List.of("hr:employee:read", "hr:leave:read"), DataScope.DEPARTMENT, deptA.getId());

        // 상태분포: ACTIVE 2 (empA deptA, empB deptB) — empC(deptC) 제외, ALL이면 3
        List<EmployeeStatusCountResponse> status = hrAnalyticsService.getStatusDistribution();
        assertThat(count(status, EmployeeStatus.ACTIVE)).isEqualTo(2L);

        // 부서별: deptC는 결과에서 제외(노출 안 함), 스코프 내 빈 부서는 보존
        List<DepartmentHeadcountResponse> byDept = hrAnalyticsService.getHeadcountByDepartment();
        assertThat(byDept).extracting(DepartmentHeadcountResponse::departmentId)
                .containsExactlyInAnyOrder(deptA.getId(), deptB.getId(), deptEmpty.getId())
                .doesNotContain(deptC.getId());

        // 직위별: posEng ACTIVE는 empA만(empC 제외), ALL이면 2
        List<PositionHeadcountResponse> byPos = hrAnalyticsService.getHeadcountByPosition();
        assertThat(posCount(byPos, posEng.getId())).isEqualTo(1L);

        // 휴가: empC(deptC)의 SICK 신청은 제외 → SICK=0, ANNUAL(empA, deptA)은 유지
        List<LeaveTypeStatResponse> leaves = hrAnalyticsService.getLeavesByType();
        assertThat(leave(leaves, LeaveType.SICK).count()).isEqualTo(0L);
        assertThat(leave(leaves, LeaveType.ANNUAL).count()).isEqualTo(2L);
    }

    // AC-8 — 권한 없으면 403(FORBIDDEN)
    @Test
    void noEmployeeReadPermission_throwsForbidden() {
        authenticate(List.of("hr:leave:read"), DataScope.ALL, null); // employee:read 없음
        assertThrows(ErpException.class, () -> hrAnalyticsService.getStatusDistribution());
        assertThrows(ErpException.class, () -> hrAnalyticsService.getHeadcountByDepartment());
        assertThrows(ErpException.class, () -> hrAnalyticsService.getHeadcountByPosition());
        assertThrows(ErpException.class, () -> hrAnalyticsService.getEmploymentTypeDistribution());
        assertThrows(ErpException.class, () -> hrAnalyticsService.getHiresTerminations(2026));
    }

    // AC-8 — 휴가 지표는 HR_LEAVE_READ 필요
    @Test
    void noLeaveReadPermission_throwsForbidden() {
        authenticate(List.of("hr:employee:read"), DataScope.ALL, null); // leave:read 없음
        assertThrows(ErpException.class, () -> hrAnalyticsService.getLeavesByType());
    }

    private void seedLeaves() {
        LeavePolicy annual = leavePolicyRepository.save(
                LeavePolicy.of("ANN", "연차", LeaveType.ANNUAL, 15, 5, true, 1));
        LeavePolicy sick = leavePolicyRepository.save(
                LeavePolicy.of("SCK", "병가", LeaveType.SICK, 10, 0, true, 1));
        approved(empA, annual, BigDecimal.valueOf(2));
        approved(empA, annual, BigDecimal.valueOf(3));
        approved(empC, sick, BigDecimal.valueOf(1));
        // PENDING — 집계 제외
        leaveRequestRepository.save(LeaveRequest.create(empA, sick,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), BigDecimal.valueOf(1), "pending"));
    }

    private void approved(Employee emp, LeavePolicy policy, BigDecimal days) {
        LeaveRequest req = LeaveRequest.create(emp, policy,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), days, "r");
        req.approve();
        leaveRequestRepository.save(req);
    }

    private long deptCount(List<DepartmentHeadcountResponse> rows, Long id) {
        return rows.stream().filter(r -> r.departmentId().equals(id)).findFirst().orElseThrow().count();
    }

    private long posCount(List<PositionHeadcountResponse> rows, Long id) {
        return rows.stream().filter(r -> r.positionId().equals(id)).findFirst().orElseThrow().count();
    }

    private long typeCount(List<EmploymentTypeCountResponse> rows, EmploymentType type) {
        return rows.stream().filter(r -> r.employmentType() == type).findFirst().orElseThrow().count();
    }

    private LeaveTypeStatResponse leave(List<LeaveTypeStatResponse> rows, LeaveType type) {
        return rows.stream().filter(r -> r.leaveType() == type).findFirst().orElseThrow();
    }
}
