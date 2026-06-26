package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.SalesTeamMember;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesTeamMemberRepository extends JpaRepository<SalesTeamMember, Long> {

    /**
     * 주어진 사용자와 같은 팀(들)에 속한 모든 멤버의 userId(본인 포함). DEPARTMENT 스코프의 owner 집합.
     * @TenantId 자동필터로 테넌트 격리(요청 단계 호출 — TenantContext 세팅됨).
     */
    @Query("SELECT DISTINCT m2.userId FROM SalesTeamMember m1 "
        + "JOIN SalesTeamMember m2 ON m1.team = m2.team "
        + "WHERE m1.userId = :userId")
    Set<String> findTeammateUserIds(@Param("userId") String userId);
}
