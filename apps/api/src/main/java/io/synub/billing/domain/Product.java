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

    /** 조직 전용 제품(예: 그룹웨어) — 회사 컨텍스트에서만 구독 가능. */
    @Column(name = "org_only", nullable = false)
    private boolean orgOnly;

    /** 제품 초기설정 온보딩 페이지 URL. 있으면 구독 후 서명 핸드오프 링크 제공. */
    @Column(name = "onboarding_url")
    private String onboardingUrl;

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
    public boolean isOrgOnly() { return orgOnly; }
    public String getOnboardingUrl() { return onboardingUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Plan> getPlans() { return plans; }
}
