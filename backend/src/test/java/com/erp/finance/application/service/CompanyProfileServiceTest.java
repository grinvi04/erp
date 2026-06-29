package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.CompanyProfileResponse;
import com.erp.finance.application.dto.CompanyProfileUpdateRequest;
import com.erp.finance.domain.model.CompanyProfile;
import com.erp.finance.domain.repository.CompanyProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

  @Mock private CompanyProfileRepository repository;
  @Mock private PermissionChecker permissionChecker;

  @InjectMocks private CompanyProfileService service;

  private static CompanyProfile sample() {
    return CompanyProfile.of("(주)무역상사", "1208147521", "홍길동", "서울시 강남구 1", "도소매", "전자제품");
  }

  @Test
  void getCompanyProfile_noSetting_returnsEmpty() {
    // AC-1: 미설정이면 전 항목 null인 빈 응답.
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

    CompanyProfileResponse result = service.getCompanyProfile();

    assertThat(result.companyName()).isNull();
    assertThat(result.businessNo()).isNull();
    assertThat(result.representative()).isNull();
    assertThat(result.address()).isNull();
    assertThat(result.businessType()).isNull();
    assertThat(result.businessItem()).isNull();
  }

  @Test
  void getCompanyProfile_existing_returnsStoredFields() {
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(sample()));

    CompanyProfileResponse result = service.getCompanyProfile();

    assertThat(result.companyName()).isEqualTo("(주)무역상사");
    assertThat(result.businessNo()).isEqualTo("1208147521");
    assertThat(result.representative()).isEqualTo("홍길동");
    assertThat(result.address()).isEqualTo("서울시 강남구 1");
    assertThat(result.businessType()).isEqualTo("도소매");
    assertThat(result.businessItem()).isEqualTo("전자제품");
  }

  @Test
  void getCompanyProfile_requiresReadPermission() {
    // AC-9: 조회는 FINANCE_READ 필요.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_READ);

    ErpException ex = assertThrows(ErpException.class, () -> service.getCompanyProfile());

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void updateCompanyProfile_firstTime_createsRow() {
    // AC-1: 미설정이면 새 행 저장.
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());
    given(repository.save(any(CompanyProfile.class))).willAnswer(inv -> inv.getArgument(0));

    CompanyProfileResponse result =
        service.updateCompanyProfile(
            new CompanyProfileUpdateRequest(
                "(주)무역상사", "1208147521", "홍길동", "서울시 강남구 1", "도소매", "전자제품"));

    assertThat(result.companyName()).isEqualTo("(주)무역상사");
    assertThat(result.businessNo()).isEqualTo("1208147521");
    assertThat(result.businessType()).isEqualTo("도소매");
    verify(repository).save(any(CompanyProfile.class));
  }

  @Test
  void updateCompanyProfile_existing_updatesInPlaceSingleRow() {
    // AC-1: 이미 1행 있으면 그 행을 갱신(새 행 저장 안 함 — 테넌트당 1행 유지).
    CompanyProfile existing = sample();
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(existing));

    CompanyProfileResponse result =
        service.updateCompanyProfile(
            new CompanyProfileUpdateRequest("(주)새상호", "1008112348", "김철수", "부산시 1", "제조", "기계"));

    assertThat(result.companyName()).isEqualTo("(주)새상호");
    assertThat(result.businessNo()).isEqualTo("1008112348");
    assertThat(existing.getCompanyName()).isEqualTo("(주)새상호");
    assertThat(existing.getRepresentative()).isEqualTo("김철수");
    assertThat(existing.getBusinessType()).isEqualTo("제조");
    verify(repository, never()).save(any(CompanyProfile.class));
  }

  @Test
  void updateCompanyProfile_optionalFieldsNull_persistsRequiredOnly() {
    // 경계: 선택 항목(대표자·주소·업태·종목)이 null이어도 필수(상호·사업자번호)만으로 저장.
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());
    given(repository.save(any(CompanyProfile.class))).willAnswer(inv -> inv.getArgument(0));

    CompanyProfileResponse result =
        service.updateCompanyProfile(
            new CompanyProfileUpdateRequest("(주)최소", "1208147521", null, null, null, null));

    assertThat(result.companyName()).isEqualTo("(주)최소");
    assertThat(result.businessNo()).isEqualTo("1208147521");
    assertThat(result.representative()).isNull();
    assertThat(result.businessItem()).isNull();
  }

  @Test
  void updateCompanyProfile_invalidBusinessNo_throwsBusinessNoInvalid() {
    // 공급자 사업자번호도 형식·체크섬 검증(거래처와 동일) — 잘못된 번호는 저장 거부.
    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                service.updateCompanyProfile(
                    new CompanyProfileUpdateRequest(
                        "(주)무역상사", "123-45-67890", null, null, null, null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_NO_INVALID);
  }

  @Test
  void updateCompanyProfile_withoutPermission_throwsForbidden() {
    // AC-9: 변경은 FINANCE_SETTING_WRITE 필요.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_SETTING_WRITE);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                service.updateCompanyProfile(
                    new CompanyProfileUpdateRequest(
                        "(주)무역상사", "1208147521", null, null, null, null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void currentCompanyProfile_existing_returnsEntity() {
    // 내부 접근자(발행 스냅샷·필수검증용) — 권한 검사 없이 현재 행 반환.
    CompanyProfile existing = sample();
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(existing));

    Optional<CompanyProfile> result = service.currentCompanyProfile();

    assertThat(result).containsSame(existing);
  }

  @Test
  void currentCompanyProfile_noSetting_returnsEmpty() {
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

    assertThat(service.currentCompanyProfile()).isEmpty();
  }
}
