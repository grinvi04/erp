package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  boolean existsByCode(String code);

  List<Account> findByParentIsNullOrderByCodeAsc();

  List<Account> findByParentIdOrderByCodeAsc(Long parentId);

  List<Account> findByAccountTypeAndIsActiveTrueOrderByCodeAsc(AccountType accountType);

  boolean existsByParentId(Long parentId);

  List<Account> findAllByOrderByCodeAsc();
}
