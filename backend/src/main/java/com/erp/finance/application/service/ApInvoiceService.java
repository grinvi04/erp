package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.CurrentUserProvider;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApInvoiceService {

    private final ApInvoiceRepository apInvoiceRepository;
    private final VendorRepository vendorRepository;
    private final CurrentUserProvider currentUserProvider;

    public ApInvoiceResponse findById(Long id) {
        return ApInvoiceResponse.from(getOrThrow(id));
    }

    public PageResponse<ApInvoiceResponse> findAll(ApInvoiceStatus status, Pageable pageable) {
        if (status != null) {
            return PageResponse.from(apInvoiceRepository.findByStatus(status, pageable).map(ApInvoiceResponse::from));
        }
        return PageResponse.from(apInvoiceRepository.findAll(pageable).map(ApInvoiceResponse::from));
    }

    public PageResponse<ApInvoiceResponse> findByVendor(Long vendorId, Pageable pageable) {
        return PageResponse.from(apInvoiceRepository.findByVendorId(vendorId, pageable).map(ApInvoiceResponse::from));
    }

    @Transactional
    public ApInvoiceResponse create(ApInvoiceCreateRequest request) {
        if (apInvoiceRepository.existsByInvoiceNo(request.invoiceNo())) {
            throw new ErpException(ErrorCode.INVOICE_NO_DUPLICATE);
        }
        Vendor vendor = vendorRepository.findById(request.vendorId())
            .orElseThrow(() -> new ErpException(ErrorCode.VENDOR_NOT_FOUND));
        ApInvoice invoice = ApInvoice.create(request.invoiceNo(), vendor, request.invoiceDate(),
            request.dueDate(), request.totalAmount(), request.currency(), request.note());
        return ApInvoiceResponse.from(apInvoiceRepository.save(invoice));
    }

    @Transactional
    public ApInvoiceResponse submit(Long id) {
        ApInvoice invoice = getOrThrow(id);
        invoice.submit();
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse approve(Long id) {
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        ApInvoice invoice = getOrThrow(id);
        invoice.approve();
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse pay(Long id, ApInvoicePayRequest request) {
        ApInvoice invoice = getOrThrow(id);
        invoice.pay(request.amount());
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse cancel(Long id) {
        ApInvoice invoice = getOrThrow(id);
        invoice.cancel();
        return ApInvoiceResponse.from(invoice);
    }

    private ApInvoice getOrThrow(Long id) {
        return apInvoiceRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.INVOICE_NOT_FOUND));
    }
}
