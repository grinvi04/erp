package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByCode(String code);
    List<Account> findByParentIsNullOrderByCodeAsc();
    List<Account> findByParentIdOrderByCodeAsc(Long parentId);
    List<Account> findByAccountTypeAndIsActiveTrueOrderByCodeAsc(AccountType accountType);
    boolean existsByParentId(Long parentId);
    List<Account> findAllByOrderByCodeAsc();
}
