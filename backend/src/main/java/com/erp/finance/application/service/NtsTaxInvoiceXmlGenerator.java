package com.erp.finance.application.service;

import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxType;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 국세청 전자세금계산서 표준 구조 XML 생성기 — 발행된 세금계산서를 well-formed XML로 변환한다. 공급자·공급받는자(사업자번호·상호·대표자·주소·업태·종목),
 * 작성일자, 품목(품목명·공급가액·세액), 합계, 세금계산서 종류(과세/영세율/면세)·청구/영수 구분을 표준 구조로 포함한다. DOM으로 생성해 well-formedness와
 * 특수문자 이스케이프를 보장한다.
 *
 * <p><b>범위</b>: 표준 구조 + 핵심 필수항목의 well-formed 문서까지. 국세청 XSD 100% 준수·전자서명·홈택스 전송은 공인 ASP 단계(범위 밖).
 */
@Component
public class NtsTaxInvoiceXmlGenerator {

  private static final String NAMESPACE = "urn:kr:or:nts:standard:TaxInvoice";
  private static final DateTimeFormatter WRITE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  public String generate(TaxInvoice taxInvoice) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

      Element root = doc.createElementNS(NAMESPACE, "TaxInvoice");
      doc.appendChild(root);

      Element header = appendChild(doc, root, "ExchangedDocument");
      appendText(doc, header, "IssueID", taxInvoice.getIssueNo());
      appendText(doc, header, "WriteDate", taxInvoice.getWriteDate().format(WRITE_DATE));
      appendText(doc, header, "PurposeCode", purposeCode(taxInvoice.getChargeType()));
      appendText(doc, header, "TaxTypeCode", taxTypeCode(taxInvoice.getTaxType()));

      appendParty(doc, root, "Invoicer", taxInvoice.getSupplier());
      appendParty(doc, root, "Invoicee", taxInvoice.getBuyer());

      Element line = appendChild(doc, root, "TradeLineItem");
      appendText(doc, line, "ItemName", taxInvoice.getItemName());
      appendText(doc, line, "SupplyAmount", won(taxInvoice.getSupplyAmount()));
      appendText(doc, line, "TaxAmount", won(taxInvoice.getVatAmount()));

      appendText(doc, root, "TotalAmount", won(taxInvoice.getTotalAmount()));

      return serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("세금계산서 XML 생성에 실패했습니다", e);
    }
  }

  private static void appendParty(Document doc, Element parent, String prefix, PartySnapshot p) {
    Element party = appendChild(doc, parent, prefix + "Party");
    appendText(doc, party, prefix + "BusinessID", p.getBusinessNo());
    appendText(doc, party, prefix + "Name", p.getCompanyName());
    appendText(doc, party, prefix + "Representative", p.getRepresentative());
    appendText(doc, party, prefix + "Address", p.getAddress());
    appendText(doc, party, prefix + "BusinessType", p.getBusinessType());
    appendText(doc, party, prefix + "BusinessClass", p.getBusinessItem());
  }

  private static Element appendChild(Document doc, Element parent, String name) {
    Element el = doc.createElementNS(NAMESPACE, name);
    parent.appendChild(el);
    return el;
  }

  private static void appendText(Document doc, Element parent, String name, String value) {
    Element el = doc.createElementNS(NAMESPACE, name);
    el.setTextContent(value != null ? value : "");
    parent.appendChild(el);
  }

  /** 청구/영수 코드 — 영수 01, 청구 02(국세청 표준). */
  private static String purposeCode(ChargeType chargeType) {
    return chargeType == ChargeType.RECEIPT ? "01" : "02";
  }

  /** 과세구분 코드 — 과세 01, 영세율 02, 면세 03. */
  private static String taxTypeCode(TaxType taxType) {
    return switch (taxType) {
      case TAXABLE -> "01";
      case ZERO_RATED -> "02";
      case EXEMPT -> "03";
    };
  }

  /** 금액은 원 단위 정수로 표기(원 미만 절사). */
  private static String won(BigDecimal amount) {
    return amount.setScale(0, RoundingMode.DOWN).toPlainString();
  }

  private static String serialize(Document doc) throws Exception {
    var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }
}
