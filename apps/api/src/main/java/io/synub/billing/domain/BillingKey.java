package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "billing_key")
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "pg_billing_key", nullable = false)
    private String pgBillingKey;

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    private String status;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected BillingKey() {}

    public BillingKey(Customer customer, String pgBillingKey, String cardCompany,
                      String cardLast4, String cardType, boolean primary) {
        this.customer = customer;
        this.pgBillingKey = pgBillingKey;
        this.cardCompany = cardCompany;
        this.cardLast4 = cardLast4;
        this.cardType = cardType;
        this.primary = primary;
        this.status = "active";
        // 기본 소유 = 등록 고객(개인). 조직 소유면 서비스에서 setOwner 로 덮어쓴다.
        this.ownerType = "customer";
        this.ownerId = customer.getId();
    }

    public void setOwner(String ownerType, Long ownerId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public String getPgBillingKey() { return pgBillingKey; }
    public String getCardCompany() { return cardCompany; }
    public String getCardLast4() { return cardLast4; }
    public String getCardType() { return cardType; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwnerType() { return ownerType; }
    public Long getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
}
