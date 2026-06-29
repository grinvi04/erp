package com.erp.finance.application.service;

import com.erp.common.bulkimport.BulkImportResult;
import com.erp.common.bulkimport.CsvBulkImport;
import com.erp.common.bulkimport.CsvReader;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.CustomerCreateRequest;
import com.erp.finance.domain.model.BusinessNoValidator;
import com.erp.finance.domain.repository.CustomerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래처(고객) CSV 대량 업로드 — 템플릿 헤더로 행을 매핑·검증(필수·사업자번호·코드 중복)하고, 전부 유효할 때만 일괄 생성한다(all-or-nothing). 생성은
 * {@link CustomerService#create}를 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerImportService {

  private static final int MAX_ROWS = 1000;
  private static final int DEFAULT_PAYMENT_TERMS = 30;
  static final List<String> HEADERS =
      List.of("코드", "업체명", "사업자번호", "대표자", "주소", "업태", "종목", "담당자명", "이메일", "전화", "결제기한");

  private final CustomerService customerService;
  private final CustomerRepository customerRepository;
  private final PermissionChecker permissionChecker;
  private final Validator validator;

  @Transactional
  public BulkImportResult importCsv(InputStream csv) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    CsvReader.CsvTable table = CsvReader.parse(csv);
    CsvBulkImport.Validated<CustomerCreateRequest> validated =
        CsvBulkImport.validate(
            table, HEADERS, MAX_ROWS, CustomerCreateRequest::code, this::toRequest);

    int total = table.rows().size();
    if (!validated.errors().isEmpty()) {
      return BulkImportResult.failed(total, validated.errors());
    }
    validated.items().forEach(customerService::create);
    return BulkImportResult.imported(total);
  }

  /** 거래처 업로드 템플릿(헤더 + 예시 1행). */
  public String template() {
    permissionChecker.require(Permission.FINANCE_READ);
    return String.join(",", HEADERS)
        + "\n"
        + "C001,(주)예시상사,1208147521,홍길동,서울시 강남구 1,도소매,전자제품,김담당,sample@example.com,02-000-0000,30\n";
  }

  private CustomerCreateRequest toRequest(CsvReader.CsvRow row) {
    String code = row.get("코드");
    String name = row.get("업체명");
    if (code.isBlank() || name.isBlank()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "코드·업체명은 필수입니다");
    }
    String businessNo = blankToNull(row.get("사업자번호"));
    BusinessNoValidator.validate(businessNo);
    if (customerRepository.existsByCode(code)) {
      throw new ErpException(ErrorCode.CUSTOMER_CODE_DUPLICATE, "이미 존재하는 코드: " + code);
    }
    CustomerCreateRequest request =
        new CustomerCreateRequest(
            code,
            name,
            businessNo,
            blankToNull(row.get("담당자명")),
            blankToNull(row.get("이메일")),
            blankToNull(row.get("전화")),
            parsePaymentTerms(row.get("결제기한")),
            null,
            blankToNull(row.get("대표자")),
            blankToNull(row.get("주소")),
            blankToNull(row.get("업태")),
            blankToNull(row.get("종목")));
    validateBean(request);
    return request;
  }

  /** API의 @Valid와 동일한 jakarta 제약(@Email·@Size·@Min 등)을 업로드 행에도 적용 — 검증 우회 방지. */
  private void validateBean(CustomerCreateRequest request) {
    var violations = validator.validate(request);
    if (!violations.isEmpty()) {
      throw new ErpException(
          ErrorCode.INVALID_INPUT,
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .collect(Collectors.joining(", ")));
    }
  }

  private static int parsePaymentTerms(String v) {
    if (v.isBlank()) {
      return DEFAULT_PAYMENT_TERMS;
    }
    try {
      return Integer.parseInt(v.trim());
    } catch (NumberFormatException e) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "결제기한은 숫자여야 합니다: " + v);
    }
  }

  private static String blankToNull(String v) {
    return v.isBlank() ? null : v;
  }
}
