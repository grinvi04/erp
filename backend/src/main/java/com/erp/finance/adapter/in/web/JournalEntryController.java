package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineResponse;
import com.erp.finance.application.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JournalEntryResponse>>> findByFiscalPeriod(
        @RequestParam Long fiscalPeriodId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(journalEntryService.findByFiscalPeriod(fiscalPeriodId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(journalEntryService.findById(id)));
    }

    @GetMapping("/{id}/lines")
    public ResponseEntity<ApiResponse<List<JournalLineResponse>>> findLines(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(journalEntryService.findLines(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JournalEntryResponse>> create(
        @Valid @RequestBody JournalEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(journalEntryService.create(request)));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(journalEntryService.post(id)));
    }
}
