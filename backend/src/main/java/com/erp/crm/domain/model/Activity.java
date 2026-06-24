package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity", schema = "crm")
public class Activity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_activity_seq")
    @SequenceGenerator(name = "crm_activity_seq", sequenceName = "crm.activity_id_seq", allocationSize = 200)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "subject", nullable = false, length = 300)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivityStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected Activity() {}

    public static Activity of(ActivityType activityType, String subject, Account account,
                              Contact contact, Opportunity opportunity, String ownerId,
                              LocalDateTime dueDate, String description) {
        Activity a = new Activity();
        a.activityType = activityType;
        a.subject = subject;
        a.account = account;
        a.contact = contact;
        a.opportunity = opportunity;
        a.ownerId = ownerId;
        a.dueDate = dueDate;
        a.status = ActivityStatus.OPEN;
        a.description = description;
        return a;
    }

    public void complete() {
        this.status = ActivityStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() { this.status = ActivityStatus.CANCELLED; }

    public boolean isOpen() { return this.status == ActivityStatus.OPEN; }

    public Long getId() { return id; }
    public ActivityType getActivityType() { return activityType; }
    public String getSubject() { return subject; }
    public Account getAccount() { return account; }
    public Contact getContact() { return contact; }
    public Opportunity getOpportunity() { return opportunity; }
    public String getOwnerId() { return ownerId; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public ActivityStatus getStatus() { return status; }
    public String getDescription() { return description; }
}
