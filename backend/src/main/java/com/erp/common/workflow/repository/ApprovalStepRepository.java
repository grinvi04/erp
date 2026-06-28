package com.erp.common.workflow.repository;

import com.erp.common.workflow.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {}
