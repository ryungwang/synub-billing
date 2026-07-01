package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    private String name;
    private String category;
    private String description;

    @Column(name = "domain_url")
    private String domainUrl;

    @Column(name = "demo_url")
    private String demoUrl;

    @Column(name = "webhook_url")
    private String webhookUrl;

    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "product")
    @OrderBy("sortOrder ASC")
    private List<Plan> plans = new ArrayList<>();

    protected Product() {}

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getServiceCode() { return serviceCode; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getDomainUrl() { return domainUrl; }
    public String getDemoUrl() { return demoUrl; }
    public String getWebhookUrl() { return webhookUrl; }
    public String getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Plan> getPlans() { return plans; }
}
