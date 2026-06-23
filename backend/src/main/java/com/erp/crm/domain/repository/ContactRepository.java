package com.erp.crm.domain.repository;

import com.erp.crm.domain.model.Contact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByAccount_IdOrderByIsPrimaryDescLastNameAsc(Long accountId);
    boolean existsByAccount_IdAndIsPrimaryTrue(Long accountId);
    boolean existsByAccount_IdAndIsPrimaryTrueAndIdNot(Long accountId, Long excludeId);
}
