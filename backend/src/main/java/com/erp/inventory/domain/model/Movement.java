package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "movement", schema = "inventory")
@SQLRestriction("deleted_at IS NULL")
public class Movement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movement_seq")
  @SequenceGenerator(
      name = "movement_seq",
      sequenceName = "inventory.movement_id_seq",
      allocationSize = 200)
  private Long id;

  @Column(name = "movement_no", nullable = false, length = 30)
  private String movementNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 20)
  private MovementType movementType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MovementStatus status;

  @Column(name = "reference_type", length = 100)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "movement_date", nullable = false)
  private LocalDate movementDate;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Column(name = "approval_request_id")
  private Long approvalRequestId;

  protected Movement() {}

  public static Movement of(
      String movementNo,
      MovementType movementType,
      LocalDate movementDate,
      String referenceType,
      Long referenceId,
      String note) {
    Movement m = new Movement();
    m.movementNo = movementNo;
    m.movementType = movementType;
    m.status = MovementStatus.DRAFT;
    m.movementDate = movementDate;
    m.referenceType = referenceType;
    m.referenceId = referenceId;
    m.note = note;
    return m;
  }

  /**
   * 직접 확정: DRAFT → CONFIRMED. 재고 조정(ADJUSTMENT)은 결재를 거쳐야 하므로 직접 확정 불가 — 작성자가 본인 조정을 즉시 반영하는 것을
   * 차단(직무분리). 입출고·이전·반품은 그대로 직접 확정.
   */
  public void confirm() {
    if (this.movementType == MovementType.ADJUSTMENT) {
      throw new ErpException(ErrorCode.MOVEMENT_REQUIRES_APPROVAL);
    }
    if (this.status != MovementStatus.DRAFT) {
      throw new ErpException(ErrorCode.MOVEMENT_NOT_DRAFT);
    }
    this.status = MovementStatus.CONFIRMED;
  }

  /**
   * 결재 상신: ADJUSTMENT DRAFT → PENDING_APPROVAL. 조정 이동만 결재 대상이며, 확정(재고 반영)은 결재 승인 후
   * PENDING_APPROVAL에서만 가능 — 직접 확정 차단.
   */
  public void submitForApproval() {
    if (this.movementType != MovementType.ADJUSTMENT) {
      throw new ErpException(ErrorCode.MOVEMENT_APPROVAL_NOT_APPLICABLE);
    }
    if (this.status != MovementStatus.DRAFT) {
      throw new ErpException(ErrorCode.MOVEMENT_NOT_DRAFT);
    }
    this.status = MovementStatus.PENDING_APPROVAL;
  }

  /** 결재 승인 확정: PENDING_APPROVAL → CONFIRMED. 결재 승인 경로(approve)에서만 호출 — 작성자가 직접 확정할 수 없다(직무분리). */
  public void confirmApproved() {
    if (this.status != MovementStatus.PENDING_APPROVAL) {
      throw new ErpException(ErrorCode.MOVEMENT_NOT_PENDING_APPROVAL);
    }
    this.status = MovementStatus.CONFIRMED;
  }

  /**
   * 결재 반려·철회 시 되돌리기: PENDING_APPROVAL → DRAFT. 되돌린 조정 이동은 수정 후 재상신할 수 있다. 상신되지 않은(PENDING_APPROVAL
   * 아님) 이동은 되돌릴 수 없다.
   */
  public void returnToDraft() {
    if (this.status != MovementStatus.PENDING_APPROVAL) {
      throw new ErpException(ErrorCode.MOVEMENT_NOT_PENDING_APPROVAL);
    }
    this.status = MovementStatus.DRAFT;
  }

  public void linkApprovalRequest(Long approvalRequestId) {
    this.approvalRequestId = approvalRequestId;
  }

  public void cancel() {
    if (this.status != MovementStatus.DRAFT) {
      throw new ErpException(ErrorCode.MOVEMENT_NOT_DRAFT);
    }
    this.status = MovementStatus.CANCELLED;
  }

  public Long getId() {
    return id;
  }

  public String getMovementNo() {
    return movementNo;
  }

  public MovementType getMovementType() {
    return movementType;
  }

  public MovementStatus getStatus() {
    return status;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public LocalDate getMovementDate() {
    return movementDate;
  }

  public String getNote() {
    return note;
  }

  public Long getApprovalRequestId() {
    return approvalRequestId;
  }
}
