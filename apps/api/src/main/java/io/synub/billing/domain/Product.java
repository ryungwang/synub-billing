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

    /** 관리자 콘솔 신규 등록(메타만). service_code는 여기서만 설정(이후 불변). 플랜·가격은 마이그레이션으로 별도 추가. */
    public Product(String serviceCode, String name, String category, String description,
                   String domainUrl, String demoUrl, String webhookUrl, String onboardingUrl,
                   int sortOrder, boolean orgOnly, String status) {
        this.serviceCode = serviceCode;
        this.name = name;
        this.category = category;
        this.description = description;
        this.domainUrl = domainUrl;
        this.demoUrl = demoUrl;
        this.webhookUrl = webhookUrl;
        this.onboardingUrl = onboardingUrl;
        this.sortOrder = sortOrder;
        this.orgOnly = orgOnly;
        this.status = status;
    }

    /** 메타데이터 수정(관리자 콘솔). service_code·플랜·가격(돈)은 불변 — 마이그레이션으로만 통제. */
    public void updateMeta(String name, String category, String description,
                           String domainUrl, String demoUrl, String webhookUrl, String onboardingUrl,
                           int sortOrder, boolean orgOnly, String status) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.domainUrl = domainUrl;
        this.demoUrl = demoUrl;
        this.webhookUrl = webhookUrl;
        this.onboardingUrl = onboardingUrl;
        this.sortOrder = sortOrder;
        this.orgOnly = orgOnly;
        this.status = status;
    }

    public Long getId() { return id; }
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
