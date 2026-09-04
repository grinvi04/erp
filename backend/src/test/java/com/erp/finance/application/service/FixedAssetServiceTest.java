package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.dto.FixedAssetResponse;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FixedAssetServiceTest {

  @Mock private FixedAssetRepository fixedAssetRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private DepreciationPostingService depreciationPostingService;
  @Mock private PermissionChecker permissionChecker;

  @InjectMocks private FixedAssetService service;

  private static FixedAssetCreateRequest request() {
    return new FixedAssetCreateRequest(
        "FA1",
        "노트북",
        LocalDate.of(2025, 1, 1),
        new BigDecimal("12000000"),
        BigDecimal.ZERO,
        60,
        DepreciationMethod.STRAIGHT_LINE,
        null,
        7L);
  }

  @Test
  void create_valid_savesAndReturns() {
    given(fixedAssetRepository.existsByCode("FA1")).willReturn(false);
    given(accountRepository.findById(7L)).willReturn(Optional.of(mock(Account.class)));
    given(fixedAssetRepository.save(any(FixedAsset.class))).willAnswer(i -> i.getArgument(0));

    FixedAssetResponse result = service.create(request());

    assertThat(result.code()).isEqualTo("FA1");
    assertThat(result.accumulatedDepreciation()).isEqualByComparingTo("0");
    assertThat(result.bookValue()).isEqualByComparingTo("12000000");
    assertThat(result.status().name()).isEqualTo("ACTIVE");
  }

  @Test
  void create_duplicateCode_throws() {
    given(fixedAssetRepository.existsByCode("FA1")).willReturn(true);

    ErpException ex = assertThrows(ErpException.class, () -> service.create(request()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
  }

  @Test
  void create_unknownAssetAccount_throws() {
    given(fixedAssetRepository.existsByCode("FA1")).willReturn(false);
    given(accountRepository.findById(7L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> service.create(request()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
  }

  @Test
  void create_withoutPermission_throwsForbidden() {
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_WRITE);

    ErpException ex = assertThrows(ErpException.class, () -> service.create(request()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void dispose_staleVersion_rejectedBeforeCatchUpSideEffects() {
    Account account = mock(Account.class);
    FixedAsset asset =
        FixedAsset.register(
            "FA1",
            "노트북",
            LocalDate.of(2025, 1, 1),
            new BigDecimal("12000000"),
            BigDecimal.ZERO,
            60,
            DepreciationMethod.STRAIGHT_LINE,
            null,
            account);
    ReflectionTestUtils.setField(asset, "version", 2L);
    given(fixedAssetRepository.findById(1L)).willReturn(Optional.of(asset));

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () ->
            service.dispose(
                1L,
                new FixedAssetDisposeRequest(
                    LocalDate.of(2025, 3, 15), BigDecimal.ZERO, null, 1L)));

    verifyNoInteractions(depreciationPostingService);
  }
}
