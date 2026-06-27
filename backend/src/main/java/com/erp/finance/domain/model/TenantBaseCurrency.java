package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * 테넌트별 기준통화 — 혼합통화 거래를 합산·환산하는 기준. 테넌트당 1행(tenant_id UNIQUE).
 * 미설정 테넌트는 서비스가 KRW를 기본으로 반환한다(행 없음 = KRW).
 */
@Entity
@Table(name = "tenant_base_currency", schema = "finance")
public class TenantBaseCurrency extends BaseEntity {

    public static final String DEFAULT_BASE_CURRENCY = "KRW";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_base_currency_seq")
    @SequenceGenerator(name = "tenant_base_currency_seq",
        sequenceName = "finance.tenant_base_currency_id_seq", allocationSize = 10)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    protected TenantBaseCurrency() {}

    public static TenantBaseCurrency of(String baseCurrency) {
        TenantBaseCurrency entity = new TenantBaseCurrency();
        entity.baseCurrency = baseCurrency;
        return entity;
    }

    public void changeBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Long getId() { return id; }
    public String getBaseCurrency() { return baseCurrency; }
}
