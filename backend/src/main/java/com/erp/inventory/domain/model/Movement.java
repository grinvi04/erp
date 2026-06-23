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

@Entity
@Table(name = "movement", schema = "inventory")
public class Movement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movement_seq")
    @SequenceGenerator(name = "movement_seq", sequenceName = "inventory.movement_id_seq", allocationSize = 200)
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

    protected Movement() {}

    public static Movement of(String movementNo, MovementType movementType,
            LocalDate movementDate, String referenceType, Long referenceId, String note) {
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

    public void confirm() {
        if (this.status != MovementStatus.DRAFT) {
            throw new ErpException(ErrorCode.MOVEMENT_NOT_DRAFT);
        }
        this.status = MovementStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status != MovementStatus.DRAFT) {
            throw new ErpException(ErrorCode.MOVEMENT_NOT_DRAFT);
        }
        this.status = MovementStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getMovementNo() { return movementNo; }
    public MovementType getMovementType() { return movementType; }
    public MovementStatus getStatus() { return status; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public LocalDate getMovementDate() { return movementDate; }
    public String getNote() { return note; }
}
