package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.LeaveBalanceResponse;
import com.erp.hr.application.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> findByEmployeeAndYear(
        @RequestParam Long employeeId,
        @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.ok(
            leaveBalanceService.findByEmployeeAndYear(employeeId, year)));
    }
}
