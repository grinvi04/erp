-- 목록 엔드포인트 하드닝: vendor/customer 필터 목록 조회의 인덱스 보강.
-- ApInvoiceRepository.findByVendorId / ArInvoiceRepository.findByCustomerId 대응.
-- 소프트삭제 행을 제외하는 부분 인덱스(기존 finance 인덱스 컨벤션과 동일).

CREATE INDEX idx_ap_invoice_tenant_vendor
    ON finance.ap_invoice (tenant_id, vendor_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_ar_invoice_tenant_customer
    ON finance.ar_invoice (tenant_id, customer_id) WHERE deleted_at IS NULL;
