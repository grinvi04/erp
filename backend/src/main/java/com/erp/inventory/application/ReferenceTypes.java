package com.erp.inventory.application;

/**
 * inventory 모듈의 참조 타입 상수 — ApprovalRequest 대상(entityType)·AuditLog 대상(entityType)을 한 곳에서 묶어 문자열 커플링
 * 오타를 컴파일 타임에 방지한다. 모듈 경계 준수상 common이 아닌 inventory에 둔다.
 */
public final class ReferenceTypes {

  public static final String STOCK_MOVEMENT = "STOCK_MOVEMENT";

  private ReferenceTypes() {}
}
