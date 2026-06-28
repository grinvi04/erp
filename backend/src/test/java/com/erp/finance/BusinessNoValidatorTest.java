package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.domain.model.BusinessNoValidator;
import org.junit.jupiter.api.Test;

class BusinessNoValidatorTest {

  @Test
  void 유효한_사업자번호는_통과한다() {
    // 1208147521 — 체크섬 유효
    assertThatCode(() -> BusinessNoValidator.validate("1208147521")).doesNotThrowAnyException();
  }

  @Test
  void 하이픈_포함_형식도_통과한다() {
    assertThatCode(() -> BusinessNoValidator.validate("120-81-47521")).doesNotThrowAnyException();
  }

  @Test
  void 빈값_null_공백은_허용한다() {
    assertThatCode(() -> BusinessNoValidator.validate(null)).doesNotThrowAnyException();
    assertThatCode(() -> BusinessNoValidator.validate("")).doesNotThrowAnyException();
    assertThatCode(() -> BusinessNoValidator.validate("   ")).doesNotThrowAnyException();
  }

  @Test
  void 체크섬이_틀리면_예외() {
    assertThatThrownBy(() -> BusinessNoValidator.validate("1208147522"))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.BUSINESS_NO_INVALID);
  }

  @Test
  void 자릿수가_10이_아니면_예외() {
    assertThatThrownBy(() -> BusinessNoValidator.validate("12345"))
        .isInstanceOf(ErpException.class);
    assertThatThrownBy(() -> BusinessNoValidator.validate("12081475210"))
        .isInstanceOf(ErpException.class);
  }
}
