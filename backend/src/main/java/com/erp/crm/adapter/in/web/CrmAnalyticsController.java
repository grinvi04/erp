package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.crm.application.dto.LeadStatusCountResponse;
import com.erp.crm.application.dto.PipelineAnalyticsResponse;
import com.erp.crm.application.service.CrmAnalyticsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/analytics")
@RequiredArgsConstructor
public class CrmAnalyticsController {

    private final CrmAnalyticsService crmAnalyticsService;

    @GetMapping("/pipeline")
    public ResponseEntity<ApiResponse<PipelineAnalyticsResponse>> getPipelineDistribution() {
        return ResponseEntity.ok(ApiResponse.ok(crmAnalyticsService.getPipelineDistribution()));
    }

    @GetMapping("/leads-by-status")
    public ResponseEntity<ApiResponse<List<LeadStatusCountResponse>>> getLeadsByStatus() {
        return ResponseEntity.ok(ApiResponse.ok(crmAnalyticsService.getLeadsByStatus()));
    }
}
