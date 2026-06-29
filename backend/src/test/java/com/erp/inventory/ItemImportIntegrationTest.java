package com.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.bulkimport.BulkImportResult;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.inventory.application.service.ItemImportService;
import com.erp.inventory.domain.model.UnitOfMeasure;
import com.erp.inventory.domain.repository.ItemRepository;
import com.erp.inventory.domain.repository.UnitOfMeasureRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 품목 CSV 대량 업로드 통합 — all-or-nothing·단위코드 조회·SKU 중복·권한을 실 DB로 검증. */
@Transactional
class ItemImportIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ItemImportService importService;
  @Autowired private ItemRepository itemRepository;
  @Autowired private UnitOfMeasureRepository uomRepository;

  private static final String HEADER =
      "SKU,품목명,설명,분류코드,단위코드,원가법,표준원가,재주문점,재주문량,최소재고,최대재고,로트추적,시리얼추적\n";

  private static InputStream csv(String body) {
    return new ByteArrayInputStream((HEADER + body).getBytes(StandardCharsets.UTF_8));
  }

  @BeforeEach
  void seedUom() {
    authenticate("seeder", "inventory:write");
    uomRepository.save(UnitOfMeasure.of("EA", "개"));
  }

  @Test
  void importCsv_allValid_createsAll() {
    authenticate("importer", "inventory:write");
    BulkImportResult result =
        importService.importCsv(
            csv(
                "SKU001,품목A,,,EA,FIFO,1000,10,50,5,100,N,N\n"
                    + "SKU002,품목B,,,EA,STANDARD,500,1,2,3,4,Y,N\n"));

    assertThat(result.importedCount()).isEqualTo(2);
    assertThat(result.errors()).isEmpty();
    assertThat(itemRepository.count()).isEqualTo(2);
  }

  @Test
  void importCsv_unknownUom_createsNothing() {
    authenticate("importer", "inventory:write");
    BulkImportResult result =
        importService.importCsv(
            csv(
                "SKU001,품목A,,,EA,FIFO,1000,10,50,5,100,N,N\n"
                    + "SKU002,품목B,,,XXX,FIFO,1,1,1,1,1,N,N\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).message()).contains("단위코드");
    assertThat(itemRepository.count()).isZero();
  }

  @Test
  void importCsv_invalidNumeric_rowError() {
    authenticate("importer", "inventory:write");
    BulkImportResult result =
        importService.importCsv(csv("SKU001,품목A,,,EA,FIFO,abc,10,50,5,100,N,N\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).message()).contains("표준원가");
  }

  @Test
  void importCsv_invalidCostMethod_rowError() {
    authenticate("importer", "inventory:write");
    BulkImportResult result =
        importService.importCsv(csv("SKU001,품목A,,,EA,틀림,1000,10,50,5,100,N,N\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors().get(0).message()).contains("원가법");
  }

  @Test
  void importCsv_dbDuplicateSku_error() {
    authenticate("importer", "inventory:write");
    // 먼저 SKU001 생성(같은 트랜잭션 내 가시) 후 동일 SKU 재업로드 → 중복.
    importService.importCsv(csv("SKU001,품목A,,,EA,FIFO,1000,10,50,5,100,N,N\n"));

    BulkImportResult result =
        importService.importCsv(csv("SKU001,중복,,,EA,FIFO,1000,10,50,5,100,N,N\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors().get(0).message()).contains("SKU");
    assertThat(itemRepository.count()).isEqualTo(1);
  }

  @Test
  void importCsv_invalidBoolean_rowError() {
    authenticate("importer", "inventory:write");
    BulkImportResult result =
        importService.importCsv(csv("SKU001,품목A,,,EA,FIFO,1000,10,50,5,100,MAYBE,N\n"));

    assertThat(result.importedCount()).isZero();
    assertThat(result.errors().get(0).message()).contains("로트추적");
    assertThat(itemRepository.count()).isZero();
  }

  @Test
  void importCsv_withoutPermission_throwsForbidden() {
    authenticate("nobody", "inventory:read");
    InputStream c = csv("SKU001,품목A,,,EA,FIFO,1000,10,50,5,100,N,N\n");

    ErpException ex = assertThrows(ErpException.class, () -> importService.importCsv(c));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void template_containsHeaders() {
    authenticate("importer", "inventory:read");
    assertThat(importService.template()).contains("SKU,품목명").contains("단위코드");
  }
}
