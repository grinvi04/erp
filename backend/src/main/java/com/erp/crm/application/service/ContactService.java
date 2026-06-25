package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.ContactCreateRequest;
import com.erp.crm.application.dto.ContactResponse;
import com.erp.crm.application.dto.ContactUpdateRequest;
import com.erp.crm.domain.model.Contact;
import com.erp.crm.domain.repository.ContactRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;
    private final CrmAccountService accountService;
    private final PermissionChecker permissionChecker;

    public List<ContactResponse> findByAccount(Long accountId) {
        permissionChecker.require(Permission.CRM_READ);
        accountService.getOrThrow(accountId);
        return contactRepository.findByAccount_IdOrderByIsPrimaryDescLastNameAsc(accountId)
                .stream().map(ContactResponse::from).toList();
    }

    public ContactResponse findById(Long id) {
        permissionChecker.require(Permission.CRM_READ);
        return ContactResponse.from(getOrThrow(id));
    }

    @Transactional
    public ContactResponse create(ContactCreateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        if (Boolean.TRUE.equals(req.isPrimary())
                && contactRepository.existsByAccount_IdAndIsPrimaryTrue(req.accountId())) {
            throw new ErpException(ErrorCode.CONTACT_PRIMARY_DUPLICATE);
        }
        Contact contact = Contact.of(
                accountService.getOrThrow(req.accountId()),
                req.lastName(), req.firstName(), req.title(), req.department(),
                req.email(), req.phone(), req.mobile(), req.isPrimary());
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse update(Long id, ContactUpdateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        Contact contact = getOrThrow(id);
        if (Boolean.TRUE.equals(req.isPrimary())
                && contactRepository.existsByAccount_IdAndIsPrimaryTrueAndIdNot(
                        contact.getAccount().getId(), id)) {
            throw new ErpException(ErrorCode.CONTACT_PRIMARY_DUPLICATE);
        }
        contact.update(req.lastName(), req.firstName(), req.title(), req.department(),
                req.email(), req.phone(), req.mobile(), req.isPrimary());
        return ContactResponse.from(contact);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.require(Permission.CRM_WRITE);
        getOrThrow(id).softDelete();
    }

    public Contact getOrThrow(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.CONTACT_NOT_FOUND));
    }
}
