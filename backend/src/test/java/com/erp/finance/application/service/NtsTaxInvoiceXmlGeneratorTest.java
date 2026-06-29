package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxType;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;

class NtsTaxInvoiceXmlGeneratorTest {

  private final NtsTaxInvoiceXmlGenerator generator = new NtsTaxInvoiceXmlGenerator();

  private static TaxInvoice taxInvoice(TaxType taxType, ChargeType chargeType, BigDecimal supply) {
    BigDecimal vat = taxType.computeVat(supply);
    TaxInvoice t =
        TaxInvoice.issue(
            7L,
            taxType,
            chargeType,
            LocalDate.of(2026, 6, 1),
            supply,
            vat,
            supply.add(vat),
            "전자부품 외",
            PartySnapshot.of("(주)공급자", "1208800344", "홍대표", "서울시 종로구 1", "제조", "전자부품"),
            PartySnapshot.of("(주)바이어", "2208612345", "이대표", "부산시 1", "도소매", "전자제품"),
            "비고");
    ReflectionTestUtils.setField(t, "id", 100L);
    t.assignIssueNo("TI-00000100");
    return t;
  }

  private static String textOf(Document doc, String tag) {
    var nodes = doc.getElementsByTagName(tag);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  private static Document parse(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    return factory
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void generate_isWellFormedXml() {
    String xml = generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000)));

    assertThatCode(() -> parse(xml)).doesNotThrowAnyException();
    assertThat(xml).startsWith("<?xml");
  }

  @Test
  void generate_includesSupplierIdentity() throws Exception {
    // AC-6: 공급자 필수항목 — 사업자번호·상호·대표자·주소·업태·종목.
    Document doc =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000))));

    assertThat(textOf(doc, "InvoicerBusinessID")).isEqualTo("1208800344");
    assertThat(textOf(doc, "InvoicerName")).isEqualTo("(주)공급자");
    assertThat(textOf(doc, "InvoicerRepresentative")).isEqualTo("홍대표");
    assertThat(textOf(doc, "InvoicerAddress")).isEqualTo("서울시 종로구 1");
    assertThat(textOf(doc, "InvoicerBusinessType")).isEqualTo("제조");
    assertThat(textOf(doc, "InvoicerBusinessClass")).isEqualTo("전자부품");
  }

  @Test
  void generate_includesBuyerIdentity() throws Exception {
    // AC-6: 공급받는자 필수항목.
    Document doc =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000))));

    assertThat(textOf(doc, "InvoiceeBusinessID")).isEqualTo("2208612345");
    assertThat(textOf(doc, "InvoiceeName")).isEqualTo("(주)바이어");
    assertThat(textOf(doc, "InvoiceeRepresentative")).isEqualTo("이대표");
    assertThat(textOf(doc, "InvoiceeBusinessType")).isEqualTo("도소매");
    assertThat(textOf(doc, "InvoiceeBusinessClass")).isEqualTo("전자제품");
  }

  @Test
  void generate_includesAmountsAndItem() throws Exception {
    // AC-6: 작성일자·품목명·공급가액·세액·합계.
    Document doc =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000))));

    assertThat(textOf(doc, "IssueID")).isEqualTo("TI-00000100");
    assertThat(textOf(doc, "WriteDate")).isEqualTo("20260601");
    assertThat(textOf(doc, "ItemName")).isEqualTo("전자부품 외");
    assertThat(textOf(doc, "SupplyAmount")).isEqualTo("1000000");
    assertThat(textOf(doc, "TaxAmount")).isEqualTo("100000");
    assertThat(textOf(doc, "TotalAmount")).isEqualTo("1100000");
  }

  @Test
  void generate_purposeCodeReflectsChargeType() throws Exception {
    // 청구=02, 영수=01.
    Document charge =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000))));
    Document receipt =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.RECEIPT, bd(1_000_000))));

    assertThat(textOf(charge, "PurposeCode")).isEqualTo("02");
    assertThat(textOf(receipt, "PurposeCode")).isEqualTo("01");
  }

  @Test
  void generate_taxTypeCodeReflectsTaxType() throws Exception {
    // 과세01·영세율02·면세03.
    Document taxable =
        parse(generator.generate(taxInvoice(TaxType.TAXABLE, ChargeType.CHARGE, bd(1_000_000))));
    Document zero =
        parse(generator.generate(taxInvoice(TaxType.ZERO_RATED, ChargeType.CHARGE, bd(1_000_000))));
    Document exempt =
        parse(generator.generate(taxInvoice(TaxType.EXEMPT, ChargeType.CHARGE, bd(1_000_000))));

    assertThat(textOf(taxable, "TaxTypeCode")).isEqualTo("01");
    assertThat(textOf(zero, "TaxTypeCode")).isEqualTo("02");
    assertThat(textOf(exempt, "TaxTypeCode")).isEqualTo("03");
    // 영세율은 세액 0.
    assertThat(textOf(zero, "TaxAmount")).isEqualTo("0");
  }

  @Test
  void generate_zeroRated_hasZeroTaxAndMatchingTotal() throws Exception {
    Document doc =
        parse(generator.generate(taxInvoice(TaxType.ZERO_RATED, ChargeType.CHARGE, bd(500_000))));

    assertThat(textOf(doc, "SupplyAmount")).isEqualTo("500000");
    assertThat(textOf(doc, "TaxAmount")).isEqualTo("0");
    assertThat(textOf(doc, "TotalAmount")).isEqualTo("500000");
  }

  private static BigDecimal bd(long v) {
    return BigDecimal.valueOf(v);
  }
}
