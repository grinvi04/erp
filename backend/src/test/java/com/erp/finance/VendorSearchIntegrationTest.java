package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.VendorRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class VendorSearchIntegrationTest extends AbstractIntegrationTest {

  @Autowired private VendorRepository vendorRepository;

  private final PageRequest pageable = PageRequest.of(0, 20);

  @BeforeEach
  void setUp() {
    vendorRepository.save(
        Vendor.of("V-ACME", "Acme Corporation", "111-11-11111", null, null, null, 30));
    vendorRepository.save(
        Vendor.of("V-GLOBEX", "Globex Trading", "222-22-22222", null, null, null, 30));
    Vendor inactive =
        Vendor.of("V-OLDACME", "Old Acme Supplier", "333-33-33333", null, null, null, 30);
    inactive.deactivate();
    vendorRepository.save(inactive);
  }

  private List<String> searchCodes(String keyword) {
    return vendorRepository.search(keyword, pageable).getContent().stream()
        .map(Vendor::getCode)
        .toList();
  }

  @Test
  void nullKeyword_returnsAllActive_excludesInactive() {
    assertThat(searchCodes(null)).containsExactlyInAnyOrder("V-ACME", "V-GLOBEX");
  }

  @Test
  void blankKeyword_returnsAllActive() {
    // 빈 문자열도 전체(활성) 반환 — LIKE %% 매칭
    assertThat(searchCodes("")).containsExactlyInAnyOrder("V-ACME", "V-GLOBEX");
  }

  @Test
  void keyword_matchesNameCaseInsensitively_andExcludesInactive() {
    // 소문자 "acme" → 활성 "Acme Corporation"만 매칭(비활성 "Old Acme Supplier"는 제외)
    assertThat(searchCodes("acme")).containsExactly("V-ACME");
  }

  @Test
  void keyword_matchesByCodePartial() {
    assertThat(searchCodes("globex")).containsExactly("V-GLOBEX");
  }

  @Test
  void keyword_matchesByBusinessNoPartial() {
    assertThat(searchCodes("222-22")).containsExactly("V-GLOBEX");
  }

  @Test
  void keyword_noMatch_returnsEmpty() {
    assertThat(searchCodes("nonexistent")).isEmpty();
  }
}
