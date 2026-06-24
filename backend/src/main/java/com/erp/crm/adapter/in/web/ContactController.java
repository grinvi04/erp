package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.crm.application.dto.ContactCreateRequest;
import com.erp.crm.application.dto.ContactResponse;
import com.erp.crm.application.dto.ContactUpdateRequest;
import com.erp.crm.application.service.ContactService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactResponse>>> findByAccount(
            @RequestParam Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.findByAccount(accountId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> create(
            @Valid @RequestBody ContactCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(contactService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContactUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
