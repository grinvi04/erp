package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.bulkimport.BulkImportResult;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.service.CustomerImportService;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.CustomerRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 거래처 CSV 대량 업로드 통합 — all-or-nothing(실패 시 전체 미생성)·중복·권한·템플릿을 실 DB로 검증. */
@Transactional
class CustomerImportIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CustomerImportService importService;
  @Autowired private CustomerRepository customerRepository;

  private static final String HEADER = "코드,업체명,사업자번호,대표자,주소,업태,종목,담당자명,이메일,전화,결제기한\n";

  private static InputStream csv(String body) {
    return new ByteArrayInputStream((HEADER + body).getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void importCsv_allValid_createsAll() {
    authenticate("importer", "finance:write");
    BulkImportResult result =
        importService.importCsv(
            csv("C1,(주)갑,1208147521,홍길동,서울 1,도소매,전자,김담당,a@b.com,02-1,30\n" + "C2,(주)을,,,,,,,,,\n"));

    assertThat(result.importedCount()).isEqualTo(2);
    assertThat(result.errors()).isEmpty();
    assertThat(customerRepository.count()).isEqualTo(2);
  }

  @Test
  void importCsv_oneInvalidBusinessNo_createsNothing() {
    authenticate("importer", "finance:write");
    BulkImportResult result =
        importService.importCsv(
            csv(
                "C1,(주)갑,1208147521,,,,,,,,\n"
                    + "C2,(주)을,123-45-67890,,,,,,,,\n" // 잘못된 사업자번호(체크섬)
                    + "C3,(주)병,,,,,,,,,\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).rowNumber()).isEqualTo(3); // 파일 3번째 줄(C2)
    assertThat(customerRepository.count()).isZero(); // 전체 미생성
  }

  @Test
  void importCsv_dbDuplicateCode_error() {
    authenticate("importer", "finance:write");
    customerRepository.save(Customer.of("C1", "기존", null, null, null, null, 30));

    BulkImportResult result = importService.importCsv(csv("C1,(주)갑,,,,,,,,,\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).message()).contains("코드");
    assertThat(customerRepository.count()).isEqualTo(1); // 기존 1건만
  }

  @Test
  void importCsv_inFileDuplicateCode_error() {
    authenticate("importer", "finance:write");
    BulkImportResult result =
        importService.importCsv(csv("C1,(주)갑,,,,,,,,,\n" + "C1,(주)을,,,,,,,,,\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).message()).contains("중복");
  }

  @Test
  void importCsv_headerMismatch_throwsInvalidInput() {
    authenticate("importer", "finance:write");
    InputStream bad = new ByteArrayInputStream("코드,틀린헤더\nC1,갑\n".getBytes(StandardCharsets.UTF_8));

    ErpException ex = assertThrows(ErpException.class, () -> importService.importCsv(bad));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void importCsv_withoutPermission_throwsForbidden() {
    authenticate("nobody", "finance:read");
    InputStream c = csv("C1,(주)갑,,,,,,,,,\n");

    ErpException ex = assertThrows(ErpException.class, () -> importService.importCsv(c));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void template_containsHeaders() {
    authenticate("importer", "finance:read");
    assertThat(importService.template()).contains("코드,업체명").contains("사업자번호");
  }
}
