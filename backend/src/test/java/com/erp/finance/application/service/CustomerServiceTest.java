package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.CustomerCreateRequest;
import com.erp.finance.application.dto.CustomerResponse;
import com.erp.finance.application.dto.CustomerUpdateRequest;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private com.erp.common.security.PermissionChecker permissionChecker;

    @Mock
    private com.erp.finance.domain.repository.AccountRepository accountRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void create_newCode_returnsCustomerResponse() {
        given(customerRepository.existsByCode("C001")).willReturn(false);
        Customer customer = Customer.of("C001", "테스트고객사", "123-45-67890",
            "홍길동", "hong@test.com", "010-1234-5678", 30);
        given(customerRepository.save(any())).willReturn(customer);

        CustomerResponse result = customerService.create(
            new CustomerCreateRequest("C001", "테스트고객사", "123-45-67890",
                "홍길동", "hong@test.com", "010-1234-5678", 30, null));

        assertThat(result.code()).isEqualTo("C001");
        assertThat(result.paymentTerms()).isEqualTo(30);
    }

    @Test
    void create_duplicateCode_throwsCustomerCodeDuplicate() {
        given(customerRepository.existsByCode("C001")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            customerService.create(new CustomerCreateRequest("C001", "테스트고객사", null, null, null, null, 0, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CUSTOMER_CODE_DUPLICATE);
    }

    @Test
    void update_found_returnsUpdatedResponse() {
        Customer customer = Customer.of("C001", "기존고객사", null, null, null, null, 0);
        given(customerRepository.findById(1L)).willReturn(Optional.of(customer));

        CustomerResponse result = customerService.update(1L,
            new CustomerUpdateRequest("변경고객사", null, null, null, null, 60, null));

        assertThat(result.name()).isEqualTo("변경고객사");
        assertThat(result.paymentTerms()).isEqualTo(60);
    }

    @Test
    void findById_notFound_throwsCustomerNotFound() {
        given(customerRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> customerService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
    }

    @Test
    void deactivate_found_deactivatesCustomer() {
        Customer customer = Customer.of("C001", "고객사", null, null, null, null, 0);
        given(customerRepository.findById(1L)).willReturn(Optional.of(customer));

        customerService.deactivate(1L);

        assertThat(customer.isActive()).isFalse();
    }
}
