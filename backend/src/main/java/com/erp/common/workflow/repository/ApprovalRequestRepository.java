package com.erp.common.workflow.repository;

import com.erp.common.workflow.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
