package io.synub.billing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    private String name;
    private String tagline;
    private int amount;
    private String currency;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle;

    /** 과금 방식: flat(정액) | per_seat(인원당, amount=1인당 단가). */
    @Column(name = "pricing_type", nullable = false)
    private String pricingType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> features = new ArrayList<>();

    @Column(name = "is_highlight", nullable = false)
    private boolean highlight;

    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Plan() {}

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getPlanCode() { return planCode; }
    public String getName() { return name; }
    public String getTagline() { return tagline; }
    public int getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getBillingCycle() { return billingCycle; }
    public String getPricingType() { return pricingType; }

    public boolean isPerSeat() { return "per_seat".equals(pricingType); }

    /** 좌석 수를 반영한 실제 청구액. 정액이면 seats 무시. long 연산으로 정수 오버플로 차단. */
    public int amountForSeats(int seats) {
        if (!isPerSeat()) return amount;
        long total = (long) amount * Math.max(1, seats);
        if (total > Integer.MAX_VALUE) {
            throw new IllegalStateException("청구 금액이 허용 범위를 초과했습니다.");
        }
        return (int) total;
    }
    public List<String> getFeatures() { return features; }
    public boolean isHighlight() { return highlight; }
    public String getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
