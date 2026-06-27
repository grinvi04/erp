package com.erp.finance.application;

/**
 * finance 모듈의 참조 타입 상수 — JournalEntry 참조(referenceType)·ApprovalRequest 대상(entityType)·
 * AuditLog 대상(entityType)을 한 곳에서 묶어 write(linkReference)와 read(findByReferenceType…)의
 * 문자열 커플링 오타를 컴파일 타임에 방지한다. 모듈 경계 준수상 common이 아닌 finance에 둔다.
 */
public final class ReferenceTypes {

    public static final String GL_ENTRY = "GL_ENTRY";
    public static final String AP_INVOICE = "AP_INVOICE";
    public static final String AP_PAYMENT = "AP_PAYMENT";
    public static final String AR_INVOICE = "AR_INVOICE";
    public static final String AR_PAYMENT = "AR_PAYMENT";

    private ReferenceTypes() {
    }
}
