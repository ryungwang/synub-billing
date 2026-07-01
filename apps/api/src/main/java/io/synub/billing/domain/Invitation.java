package io.synub.billing.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** 조직 멤버 초대. 이메일로 pending 생성 → 대상이 로그인 후 수락하면 멤버십으로 전환. */
@Entity
@Table(name = "invitation")
public class Invitation {

    public static final String PENDING = "pending";
    public static final String ACCEPTED = "accepted";
    public static final String DECLINED = "declined";
    public static final String CANCELED = "canceled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    private String email;

    private String role;

    private String status;

    @Column(name = "invited_by_customer_id")
    private Long invitedByCustomerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /** 초대 유효기간(일). */
    public static final long EXPIRY_DAYS = 7;

    protected Invitation() {}

    public Invitation(Long organizationId, String email, String role, Long invitedByCustomerId) {
        this.organizationId = organizationId;
        this.email = email;
        this.role = role;
        this.status = PENDING;
        this.invitedByCustomerId = invitedByCustomerId;
        this.expiresAt = Instant.now().plus(java.time.Duration.ofDays(EXPIRY_DAYS));
    }

    public boolean isPending() { return PENDING.equals(status); }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public Instant getExpiresAt() { return expiresAt; }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getInvitedByCustomerId() { return invitedByCustomerId; }
    public Instant getCreatedAt() { return createdAt; }
}
