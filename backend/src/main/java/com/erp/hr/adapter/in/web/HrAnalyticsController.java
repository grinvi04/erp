package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.DepartmentHeadcountResponse;
import com.erp.hr.application.dto.EmployeeStatusCountResponse;
import com.erp.hr.application.dto.EmploymentTypeCountResponse;
import com.erp.hr.application.dto.LeaveTypeStatResponse;
import com.erp.hr.application.dto.MonthlyHiresTerminationsResponse;
import com.erp.hr.application.dto.PositionHeadcountResponse;
import com.erp.hr.application.service.HrAnalyticsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/analytics")
@RequiredArgsConstructor
public class HrAnalyticsController {

    private final HrAnalyticsService hrAnalyticsService;

    @GetMapping("/status-distribution")
    public ResponseEntity<ApiResponse<List<EmployeeStatusCountResponse>>> getStatusDistribution() {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getStatusDistribution()));
    }

    @GetMapping("/by-department")
    public ResponseEntity<ApiResponse<List<DepartmentHeadcountResponse>>> getHeadcountByDepartment() {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getHeadcountByDepartment()));
    }

    @GetMapping("/by-position")
    public ResponseEntity<ApiResponse<List<PositionHeadcountResponse>>> getHeadcountByPosition() {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getHeadcountByPosition()));
    }

    @GetMapping("/by-employment-type")
    public ResponseEntity<ApiResponse<List<EmploymentTypeCountResponse>>> getEmploymentTypeDistribution() {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getEmploymentTypeDistribution()));
    }

    @GetMapping("/hires-terminations")
    public ResponseEntity<ApiResponse<List<MonthlyHiresTerminationsResponse>>> getHiresTerminations(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getHiresTerminations(year)));
    }

    @GetMapping("/leaves-by-type")
    public ResponseEntity<ApiResponse<List<LeaveTypeStatResponse>>> getLeavesByType() {
        return ResponseEntity.ok(ApiResponse.ok(hrAnalyticsService.getLeavesByType()));
    }
}
