package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "pg_payment_id")
    private String pgPaymentId;

    private int amount;
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(Subscription subscription, String pgPaymentId, int amount, String status,
                   String failureReason, String receiptNo, Instant paidAt) {
        this.subscription = subscription;
        this.pgPaymentId = pgPaymentId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.receiptNo = receiptNo;
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public Subscription getSubscription() { return subscription; }
    public String getPgPaymentId() { return pgPaymentId; }
    public int getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getReceiptNo() { return receiptNo; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
}
