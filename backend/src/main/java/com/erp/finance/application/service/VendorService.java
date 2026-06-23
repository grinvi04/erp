package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.VendorCreateRequest;
import com.erp.finance.application.dto.VendorResponse;
import com.erp.finance.application.dto.VendorUpdateRequest;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;

    public PageResponse<VendorResponse> findAll(Pageable pageable) {
        return PageResponse.from(vendorRepository.findByIsActiveTrue(pageable).map(VendorResponse::from));
    }

    public VendorResponse findById(Long id) {
        return VendorResponse.from(getOrThrow(id));
    }

    @Transactional
    public VendorResponse create(VendorCreateRequest request) {
        if (vendorRepository.existsByCode(request.code())) {
            throw new ErpException(ErrorCode.VENDOR_CODE_DUPLICATE);
        }
        Vendor vendor = Vendor.of(request.code(), request.name(), request.businessNo(),
            request.contactName(), request.contactEmail(), request.contactPhone(), request.paymentTerms());
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponse update(Long id, VendorUpdateRequest request) {
        Vendor vendor = getOrThrow(id);
        vendor.update(request.name(), request.businessNo(), request.contactName(),
            request.contactEmail(), request.contactPhone(), request.paymentTerms());
        return VendorResponse.from(vendor);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).deactivate();
    }

    private Vendor getOrThrow(Long id) {
        return vendorRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.VENDOR_NOT_FOUND));
    }
}
