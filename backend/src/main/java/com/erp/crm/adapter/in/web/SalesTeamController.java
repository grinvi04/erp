package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.crm.application.dto.SalesTeamCreateRequest;
import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.application.dto.SalesTeamUpdateRequest;
import com.erp.crm.application.dto.TeamMemberRequest;
import com.erp.crm.application.service.SalesTeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/sales-teams")
@RequiredArgsConstructor
public class SalesTeamController {

    private final SalesTeamService salesTeamService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesTeamResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(salesTeamService.listTeams()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesTeamResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(salesTeamService.getTeam(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesTeamResponse>> create(
            @Valid @RequestBody SalesTeamCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(salesTeamService.createTeam(request.code(), request.name())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesTeamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SalesTeamUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(salesTeamService.updateTeam(id, request.name(), request.version())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesTeamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<SalesTeamResponse>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(salesTeamService.addMember(id, request.userId())));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<SalesTeamResponse>> removeMember(
            @PathVariable Long id,
            @PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(salesTeamService.removeMember(id, userId)));
    }
}
