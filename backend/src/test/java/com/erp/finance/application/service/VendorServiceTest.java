package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.VendorCreateRequest;
import com.erp.finance.application.dto.VendorResponse;
import com.erp.finance.application.dto.VendorUpdateRequest;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.VendorRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

  @Mock private VendorRepository vendorRepository;

  @Mock private com.erp.common.security.PermissionChecker permissionChecker;

  @Mock private com.erp.finance.domain.repository.AccountRepository accountRepository;

  @InjectMocks private VendorService vendorService;

  @Test
  void create_newCode_returnsVendorResponse() {
    given(vendorRepository.existsByCode("V001")).willReturn(false);
    Vendor vendor =
        Vendor.of("V001", "테스트공급사", "123-45-67890", "홍길동", "hong@test.com", "010-1234-5678", 30);
    given(vendorRepository.save(any())).willReturn(vendor);

    VendorResponse result =
        vendorService.create(
            new VendorCreateRequest(
                "V001",
                "테스트공급사",
                "123-45-67890",
                "홍길동",
                "hong@test.com",
                "010-1234-5678",
                30,
                null));

    assertThat(result.code()).isEqualTo("V001");
    assertThat(result.paymentTerms()).isEqualTo(30);
  }

  @Test
  void create_duplicateCode_throwsVendorCodeDuplicate() {
    given(vendorRepository.existsByCode("V001")).willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                vendorService.create(
                    new VendorCreateRequest("V001", "테스트공급사", null, null, null, null, 0, null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VENDOR_CODE_DUPLICATE);
  }

  @Test
  void update_found_returnsUpdatedResponse() {
    Vendor vendor = Vendor.of("V001", "기존공급사", null, null, null, null, 0);
    ReflectionTestUtils.setField(vendor, "version", 3L);
    given(vendorRepository.findById(1L)).willReturn(Optional.of(vendor));

    VendorResponse result =
        vendorService.update(
            1L, new VendorUpdateRequest("변경공급사", null, null, null, null, 60, null, 3L));

    assertThat(result.name()).isEqualTo("변경공급사");
    assertThat(result.paymentTerms()).isEqualTo(60);
    assertThat(result.version()).isEqualTo(3L);
  }

  @Test
  void update_versionMismatch_throwsOptimisticLockConflict() {
    Vendor vendor = Vendor.of("V001", "기존공급사", null, null, null, null, 0);
    ReflectionTestUtils.setField(vendor, "version", 5L);
    given(vendorRepository.findById(1L)).willReturn(Optional.of(vendor));

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () ->
            vendorService.update(
                1L, new VendorUpdateRequest("변경공급사", null, null, null, null, 60, null, 3L)));
  }

  @Test
  void findById_notFound_throwsVendorNotFound() {
    given(vendorRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> vendorService.findById(99L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_FOUND);
  }

  @Test
  void deactivate_found_deactivatesVendor() {
    Vendor vendor = Vendor.of("V001", "공급사", null, null, null, null, 0);
    given(vendorRepository.findById(1L)).willReturn(Optional.of(vendor));

    vendorService.deactivate(1L);

    assertThat(vendor.isActive()).isFalse();
  }
}
