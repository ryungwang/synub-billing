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

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Organization() {}

    public Organization(Long companyId, String name) {
        this.companyId = companyId;
        this.name = name;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
