package io.synub.billing.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** 사람(customer) ↔ 조직 연결 + 역할. 역할: owner | billing_manager | member. */
@Entity
@Table(name = "membership")
public class Membership {

    public static final String OWNER = "owner";
    public static final String BILLING_MANAGER = "billing_manager";
    public static final String MEMBER = "member";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    private String role;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Membership() {}

    public Membership(Long organizationId, Long customerId, String role) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.role = role;
    }

    /** 결제 관리 권한이 있는 역할인가(owner/billing_manager). */
    public boolean canManageBilling() {
        return OWNER.equals(role) || BILLING_MANAGER.equals(role);
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getCustomerId() { return customerId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
}
