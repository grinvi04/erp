package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.crm.application.dto.CrmSummaryResponse;
import com.erp.crm.application.service.CrmSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/summary")
@RequiredArgsConstructor
public class CrmSummaryController {

    private final CrmSummaryService crmSummaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<CrmSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(crmSummaryService.getSummary()));
    }
}
