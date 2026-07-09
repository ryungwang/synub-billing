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

    /**
     * 예약된 플랜 변경(다운그레이드·결제주기 변경). 다음 갱신 결제 성공 시 plan으로 스왑되고 비워진다.
     * entitlement는 이 값을 무시하고 현재 plan을 읽는다 — 이용 중 기간엔 결제한 플랜을 그대로 보장.
     * 업그레이드는 즉시 전환(차액 즉시청구)이라 이 값을 쓰지 않는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_plan_id")
    private Plan pendingPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id")   // 무상(complimentary) 구독은 결제수단 없음(null)
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

    /** 비례정산 크레딧 잔액(원). 다음 청구에서 차감. 좌석 감소 시 적립. */
    @Column(name = "credit_balance", nullable = false)
    private int creditBalance = 0;

    /** 무상 구독(개발사 등) — 청구 제외, billing_key 없음. */
    @Column(nullable = false)
    private boolean complimentary = false;

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

    /** 무상 구독(개발사 등) — 결제수단 없이 즉시 active. owner는 서비스에서 org로 설정. */
    public static Subscription complimentary(Customer customer, Plan plan,
                                             Instant startedAt, LocalDate nextBillingDate) {
        Subscription s = new Subscription();
        s.customer = customer;
        s.plan = plan;
        s.billingKey = null;
        s.status = "active";
        s.startedAt = startedAt;
        s.nextBillingDate = nextBillingDate;
        s.cancelAtPeriodEnd = false;
        s.complimentary = true;
        s.ownerType = "customer";
        s.ownerId = customer.getId();
        return s;
    }

    public boolean isComplimentary() { return complimentary; }

    public void setOwner(String ownerType, Long ownerId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public Plan getPendingPlan() { return pendingPlan; }
    public void setPendingPlan(Plan pendingPlan) { this.pendingPlan = pendingPlan; }
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
    public int getCreditBalance() { return creditBalance; }
    public void setCreditBalance(int creditBalance) { this.creditBalance = Math.max(0, creditBalance); }
    /** 좌석 수 반영 실제 청구액(크레딧 차감 전 총액). */
    public int chargeAmount() { return plan.amountForSeats(seats); }
    public String getOwnerType() { return ownerType; }
    public Long getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
}
