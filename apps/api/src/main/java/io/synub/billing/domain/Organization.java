package io.synub.billing.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** 구매 회사(고객사). 개인/회사 컨텍스트의 "회사" 단위. */
@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    private String name;

    @Column(name = "business_no")
    private String businessNo;

    @Column(name = "business_doc")
    private String businessDoc;

    /** pending | verified | rejected */
    @Column(name = "verify_status", nullable = false)
    private String verifyStatus = "pending";

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Organization() {}

    public Organization(Long companyId, String name) {
        this.companyId = companyId;
        this.name = name;
    }

    /** 사업자 정보 등록(생성 시). 소유권 심사 전이라 pending. */
    public void submitBusiness(String businessNo, String businessDoc) {
        this.businessNo = businessNo;
        this.businessDoc = businessDoc;
        this.verifyStatus = "pending";
        this.rejectReason = null;
    }

    public void approve(Instant when) {
        this.verifyStatus = "verified";
        this.verifiedAt = when;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        this.verifyStatus = "rejected";
        this.rejectReason = reason;
    }

    public boolean isVerified() { return "verified".equals(verifyStatus); }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getBusinessNo() { return businessNo; }
    public String getBusinessDoc() { return businessDoc; }
    public String getVerifyStatus() { return verifyStatus; }
    public String getRejectReason() { return rejectReason; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
