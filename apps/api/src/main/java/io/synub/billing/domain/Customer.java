package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String email;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {}

    public Customer(Long companyId, String externalId, String email) {
        this.companyId = companyId;
        this.externalId = externalId;
        this.email = email;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
}
