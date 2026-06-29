package com.erp.inventory.application.service;

import com.erp.common.bulkimport.BulkImportResult;
import com.erp.common.bulkimport.CsvBulkImport;
import com.erp.common.bulkimport.CsvReader;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.inventory.application.dto.ItemCreateRequest;
import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.repository.ItemCategoryRepository;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 품목 CSV 대량 업로드 — 템플릿 헤더로 행을 매핑·검증(필수·SKU 중복·단위/분류 코드 조회·원가법·수치)하고, 전부 유효할 때만 일괄
 * 생성한다(all-or-nothing). 단위·분류는 코드로 참조(내부 ID 노출 안 함). 생성은 {@link ItemService#create}를 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class ItemImportService {

  private static final int MAX_ROWS = 1000;
  static final List<String> HEADERS =
      List.of(
          "SKU", "품목명", "설명", "분류코드", "단위코드", "원가법", "표준원가", "재주문점", "재주문량", "최소재고", "최대재고", "로트추적",
          "시리얼추적");

  private final ItemService itemService;
  private final ItemRepository itemRepository;
  private final UnitOfMeasureRepository unitOfMeasureRepository;
  private final ItemCategoryRepository itemCategoryRepository;
  private final PermissionChecker permissionChecker;
  private final Validator validator;

  @Transactional
  public BulkImportResult importCsv(InputStream csv) {
    permissionChecker.require(Permission.INVENTORY_WRITE);
    CsvReader.CsvTable table = CsvReader.parse(csv);
    CsvBulkImport.Validated<ItemCreateRequest> validated =
        CsvBulkImport.validate(
            table, HEADERS, MAX_ROWS, r -> r.sku().toUpperCase(), this::toRequest);

    int total = table.rows().size();
    if (!validated.errors().isEmpty()) {
      return BulkImportResult.failed(total, validated.errors());
    }
    validated.items().forEach(itemService::create);
    return BulkImportResult.imported(total);
  }

  /** 품목 업로드 템플릿(헤더 + 예시 1행). */
  public String template() {
    permissionChecker.require(Permission.INVENTORY_READ);
    return String.join(",", HEADERS) + "\n" + "SKU001,예시품목,설명,,EA,FIFO,1000,10,50,5,100,N,N\n";
  }

  private ItemCreateRequest toRequest(CsvReader.CsvRow row) {
    String sku = row.get("SKU");
    String name = row.get("품목명");
    if (sku.isBlank() || name.isBlank()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "SKU·품목명은 필수입니다");
    }
    if (itemRepository.existsBySku(sku.toUpperCase())) {
      throw new ErpException(ErrorCode.ITEM_SKU_DUPLICATE, "이미 존재하는 SKU: " + sku);
    }
    Long uomId = resolveUomId(row.get("단위코드"));
    Long categoryId = resolveCategoryId(row.get("분류코드"));
    ItemCreateRequest request =
        new ItemCreateRequest(
            sku,
            name,
            blankToNull(row.get("설명")),
            categoryId,
            uomId,
            parseCostMethod(row.get("원가법")),
            parseDecimal(row.get("표준원가"), "표준원가"),
            parseDecimal(row.get("재주문점"), "재주문점"),
            parseDecimal(row.get("재주문량"), "재주문량"),
            parseDecimal(row.get("최소재고"), "최소재고"),
            parseDecimal(row.get("최대재고"), "최대재고"),
            parseBool(row.get("로트추적"), "로트추적"),
            parseBool(row.get("시리얼추적"), "시리얼추적"));
    var violations = validator.validate(request);
    if (!violations.isEmpty()) {
      throw new ErpException(
          ErrorCode.INVALID_INPUT,
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .collect(Collectors.joining(", ")));
    }
    return request;
  }

  private Long resolveUomId(String code) {
    if (code.isBlank()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "단위코드는 필수입니다");
    }
    return unitOfMeasureRepository
        .findByCode(code.toUpperCase())
        .orElseThrow(() -> new ErpException(ErrorCode.UOM_NOT_FOUND, "단위코드 없음: " + code))
        .getId();
  }

  private Long resolveCategoryId(String code) {
    if (code.isBlank()) {
      return null;
    }
    return itemCategoryRepository
        .findByCode(code)
        .orElseThrow(() -> new ErpException(ErrorCode.ITEM_CATEGORY_NOT_FOUND, "분류코드 없음: " + code))
        .getId();
  }

  private static CostMethod parseCostMethod(String v) {
    try {
      return CostMethod.valueOf(v.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ErpException(
          ErrorCode.INVALID_INPUT, "원가법은 FIFO/LIFO/WEIGHTED_AVG/STANDARD 중 하나여야 합니다: " + v);
    }
  }

  private static BigDecimal parseDecimal(String v, String label) {
    try {
      BigDecimal d = new BigDecimal(v.trim());
      if (d.signum() < 0) {
        throw new ErpException(ErrorCode.INVALID_INPUT, label + "은(는) 0 이상이어야 합니다: " + v);
      }
      return d;
    } catch (NumberFormatException e) {
      throw new ErpException(ErrorCode.INVALID_INPUT, label + "은(는) 숫자여야 합니다: " + v);
    }
  }

  private static boolean parseBool(String v, String label) {
    String s = v.trim();
    if (s.isEmpty()
        || s.equalsIgnoreCase("N")
        || s.equalsIgnoreCase("NO")
        || s.equals("0")
        || s.equals("아니오")) {
      return false;
    }
    if (s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("YES") || s.equals("1") || s.equals("예")) {
      return true;
    }
    throw new ErpException(ErrorCode.INVALID_INPUT, label + "은(는) Y 또는 N이어야 합니다: " + v);
  }

  private static String blankToNull(String v) {
    return v.isBlank() ? null : v;
  }
}
