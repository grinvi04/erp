package com.erp.common.workflow;

import com.erp.common.response.ApiResponse;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalInboxController {

    private final ApprovalInboxService approvalInboxService;

    /** 내가 처리할 대기 결재. */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApprovalSummaryResponse>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(approvalInboxService.pendingForCurrentUser()));
    }

    /** 내가 상신한 결재. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ApprovalSummaryResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok(approvalInboxService.requestedByCurrentUser()));
    }
}
