package com.erp.hr.application.service;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.DataScope;
import com.erp.common.security.DataScopeProvider;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.repository.DepartmentRepository;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * HR 직원 조회의 데이터 스코프 필터 — 모든 직원 조회 진입점이 공통으로 사용한다
 * (auth-standards: 화면별 수작업 필터 금지, 리포지토리 공통 필터). 부서(common 미참조)는
 * HR 개념이라 이 resolver가 HR 모듈에 위치한다.
 */
@Component
@RequiredArgsConstructor
public class HrDataScopeResolver {

    private final DataScopeProvider dataScopeProvider;
    private final CurrentUserProvider currentUserProvider;
    private final DepartmentRepository departmentRepository;

    /** 현재 사용자의 스코프에 맞는 Employee 조회 제약. ALL이면 제약 없음. */
    public Specification<Employee> employeeScope() {
        DataScope scope = dataScopeProvider.getDataScope();
        return switch (scope) {
            case ALL -> Specification.where(null);
            case SELF -> {
                String sub = currentUserProvider.getCurrentUserId();
                yield (root, query, cb) ->
                        sub == null ? cb.disjunction() : cb.equal(root.get("userId"), sub);
            }
            case DEPARTMENT -> {
                Long deptId = dataScopeProvider.getDepartmentId();
                if (deptId == null) {
                    yield (root, query, cb) -> cb.disjunction();
                }
                Set<Long> deptIds = selfAndDescendantDeptIds(deptId);
                yield (root, query, cb) -> root.get("department").get("id").in(deptIds);
            }
        };
    }

    /** 직원 연관 데이터(휴가신청 등) 조회의 스코프 제약 — 소속 직원의 부서/본인 기준. */
    public Specification<LeaveRequest> leaveRequestScope() {
        DataScope scope = dataScopeProvider.getDataScope();
        return switch (scope) {
            case ALL -> Specification.where(null);
            case SELF -> {
                String sub = currentUserProvider.getCurrentUserId();
                yield (root, query, cb) ->
                        sub == null ? cb.disjunction() : cb.equal(root.get("employee").get("userId"), sub);
            }
            case DEPARTMENT -> {
                Long deptId = dataScopeProvider.getDepartmentId();
                if (deptId == null) {
                    yield (root, query, cb) -> cb.disjunction();
                }
                Set<Long> deptIds = selfAndDescendantDeptIds(deptId);
                yield (root, query, cb) ->
                        root.get("employee").get("department").get("id").in(deptIds);
            }
        };
    }

    /**
     * 집계(GROUP BY) 쿼리용 파라미터 스코프 — {@link Specification}을 쓸 수 없는 @Query 집계에 결합한다.
     * 쿼리는 {@code (:unscoped = true OR e.userId = :selfUserId OR e.department.id IN :deptIds)}로 적용.
     * 빈 컬렉션 IN·널 비교를 피하려고 deptIds는 항상 비지 않게(매칭 없는 센티넬 {@code -1L}),
     * selfUserId는 항상 널이 아니게(매칭 없는 빈 문자열) 정규화한다. 휴가 집계는 {@code lr.employee.*} 경로로 동일 적용.
     */
    public record EmployeeAnalyticsScope(boolean unscoped, Set<Long> deptIds, String selfUserId) {}

    private static final Set<Long> NO_DEPT = Set.of(-1L);
    private static final String NO_USER = "";

    /** 현재 사용자의 집계 스코프. ALL=unscoped, SELF=본인 userId, DEPARTMENT=자기+하위부서. */
    public EmployeeAnalyticsScope employeeAnalyticsScope() {
        DataScope scope = dataScopeProvider.getDataScope();
        return switch (scope) {
            case ALL -> new EmployeeAnalyticsScope(true, NO_DEPT, NO_USER);
            case SELF -> {
                String sub = currentUserProvider.getCurrentUserId();
                yield new EmployeeAnalyticsScope(false, NO_DEPT, sub != null ? sub : NO_USER);
            }
            case DEPARTMENT -> {
                Long deptId = dataScopeProvider.getDepartmentId();
                if (deptId == null) {
                    yield new EmployeeAnalyticsScope(false, NO_DEPT, NO_USER);
                }
                yield new EmployeeAnalyticsScope(false, selfAndDescendantDeptIds(deptId), NO_USER);
            }
        };
    }

    /** 직원 id가 현재 사용자 스코프 안에 있는지 — 직원 키 엔드포인트(계약·휴가잔여 등) 검사용. */
    public boolean isEmployeeInScope(Employee employee) {
        return isInScope(employee);
    }

    /** 단건 직원이 현재 스코프 안에 있는지(findById 등 단건 접근 검사용). */
    public boolean isInScope(Employee employee) {
        DataScope scope = dataScopeProvider.getDataScope();
        return switch (scope) {
            case ALL -> true;
            case SELF -> {
                String sub = currentUserProvider.getCurrentUserId();
                yield sub != null && sub.equals(employee.getUserId());
            }
            case DEPARTMENT -> {
                Long deptId = dataScopeProvider.getDepartmentId();
                yield deptId != null
                        && selfAndDescendantDeptIds(deptId).contains(employee.getDepartment().getId());
            }
        };
    }

    /** 부서 트리 BFS — 자기 부서 + 모든 하위 부서 id. (findByParentId는 테넌트 필터됨) */
    private Set<Long> selfAndDescendantDeptIds(Long rootId) {
        Set<Long> ids = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (ids.add(id)) {
                departmentRepository.findByParentId(id).forEach(child -> queue.add(child.getId()));
            }
        }
        return ids;
    }
}
