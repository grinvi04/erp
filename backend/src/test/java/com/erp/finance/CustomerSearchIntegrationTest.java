package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CustomerSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    private final PageRequest pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        customerRepository.save(Customer.of("C-ACME", "Acme Corporation", "111-11-11111",
                null, null, null, 30));
        customerRepository.save(Customer.of("C-GLOBEX", "Globex Trading", "222-22-22222",
                null, null, null, 30));
        Customer inactive = Customer.of("C-OLDACME", "Old Acme Buyer", "333-33-33333",
                null, null, null, 30);
        inactive.deactivate();
        customerRepository.save(inactive);
    }

    private List<String> searchCodes(String keyword) {
        return customerRepository.search(keyword, pageable).getContent().stream()
                .map(Customer::getCode).toList();
    }

    @Test
    void nullKeyword_returnsAllActive_excludesInactive() {
        assertThat(searchCodes(null)).containsExactlyInAnyOrder("C-ACME", "C-GLOBEX");
    }

    @Test
    void blankKeyword_returnsAllActive() {
        assertThat(searchCodes("")).containsExactlyInAnyOrder("C-ACME", "C-GLOBEX");
    }

    @Test
    void keyword_matchesNameCaseInsensitively_andExcludesInactive() {
        assertThat(searchCodes("acme")).containsExactly("C-ACME");
    }

    @Test
    void keyword_matchesByCodePartial() {
        assertThat(searchCodes("globex")).containsExactly("C-GLOBEX");
    }

    @Test
    void keyword_matchesByBusinessNoPartial() {
        assertThat(searchCodes("222-22")).containsExactly("C-GLOBEX");
    }

    @Test
    void keyword_noMatch_returnsEmpty() {
        assertThat(searchCodes("nonexistent")).isEmpty();
    }
}
