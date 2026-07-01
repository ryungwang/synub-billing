package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false)
    private BillingKey billingKey;

    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** 좌석 수(인원당 과금 plan에서만 의미). 정액 plan은 1로 취급. */
    @Column(nullable = false)
    private int seats = 1;

    /** 소유 스코프: 'customer' | 'organization'. 기본은 생성 고객 개인 소유. */
    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Subscription() {}

    public Subscription(Customer customer, Plan plan, BillingKey billingKey,
                        String status, Instant startedAt, LocalDate nextBillingDate) {
        this.customer = customer;
        this.plan = plan;
        this.billingKey = billingKey;
        this.status = status;
        this.startedAt = startedAt;
        this.nextBillingDate = nextBillingDate;
        this.cancelAtPeriodEnd = true;
        // 기본 소유 = 생성 고객(개인). 조직 소유면 서비스에서 setOwner 로 덮어쓴다.
        this.ownerType = "customer";
        this.ownerId = customer.getId();
    }

    public void setOwner(String ownerType, Long ownerId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public BillingKey getBillingKey() { return billingKey; }
    public void setBillingKey(BillingKey billingKey) { this.billingKey = billingKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(LocalDate d) { this.nextBillingDate = d; }
    public Instant getCanceledAt() { return canceledAt; }
    public void setCanceledAt(Instant t) { this.canceledAt = t; }
    public boolean isCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(boolean v) { this.cancelAtPeriodEnd = v; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = Math.max(1, seats); }
    /** 좌석 수 반영 실제 청구액. */
    public int chargeAmount() { return plan.amountForSeats(seats); }
    public String getOwnerType() { return ownerType; }
    public Long getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
}
