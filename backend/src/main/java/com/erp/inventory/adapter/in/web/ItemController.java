package com.erp.inventory.adapter.in.web;

import com.erp.common.bulkimport.BulkImportResult;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.ItemCreateRequest;
import com.erp.inventory.application.dto.ItemResponse;
import com.erp.inventory.application.dto.ItemUpdateRequest;
import com.erp.inventory.application.service.ItemImportService;
import com.erp.inventory.application.service.ItemService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory/items")
@RequiredArgsConstructor
public class ItemController {

  private final ItemService itemService;
  private final ItemImportService itemImportService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> findAll(
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String keyword,
      Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(itemService.findAll(categoryId, keyword, pageable)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ItemResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(itemService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ItemResponse>> create(
      @Valid @RequestBody ItemCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(itemService.create(req)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ItemResponse>> update(
      @PathVariable Long id, @Valid @RequestBody ItemUpdateRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(itemService.update(id, req)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
    itemService.deactivate(id);
    return ResponseEntity.ok(ApiResponse.ok());
  }

  /** CSV 대량 업로드 — 전부 유효할 때만 일괄 생성, 실패 시 행별 사유 리포트. */
  @PostMapping("/import")
  public ResponseEntity<ApiResponse<BulkImportResult>> importCsv(
      @RequestParam("file") MultipartFile file) {
    try {
      return ResponseEntity.ok(ApiResponse.ok(itemImportService.importCsv(file.getInputStream())));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 업로드 템플릿(CSV) 다운로드. */
  @GetMapping("/import/template")
  public ResponseEntity<String> importTemplate() {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"item-template.csv\"")
        .body("﻿" + itemImportService.template());
  }
}
