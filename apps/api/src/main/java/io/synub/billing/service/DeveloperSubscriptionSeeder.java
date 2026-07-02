package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.domain.Plan;
import io.synub.billing.domain.Product;
import io.synub.billing.domain.Subscription;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.PlanRepository;
import io.synub.billing.repo.ProductRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 연동 규칙: 개발사(운영사)는 등록된 모든 서비스의 최고 플랜을 무상 구독한다.
 * 기동 후 개발사 org(app.developer-org-code)에 대해, 각 제품의 최고가 플랜으로 complimentary·active 구독을
 * 보장한다(멱등 — 이미 active 구독이 있으면 skip). 새 제품이 등록되면 다음 기동 시 자동 구독된다.
 * 무상 구독은 결제수단 없이(billing_key null) active이며, 스케줄러 청구 대상에서 제외된다(findDue).
 */
@Component
public class DeveloperSubscriptionSeeder {

    private static final Logger log = LoggerFactory.getLogger(DeveloperSubscriptionSeeder.class);

    @Value("${app.developer-org-code:}")
    private String developerOrgCode;

    private final OrganizationRepository organizations;
    private final ProductRepository products;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final MembershipRepository memberships;
    private final CustomerRepository customers;

    public DeveloperSubscriptionSeeder(OrganizationRepository organizations, ProductRepository products,
                                       PlanRepository plans, SubscriptionRepository subscriptions,
                                       MembershipRepository memberships, CustomerRepository customers) {
        this.organizations = organizations;
        this.products = products;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.memberships = memberships;
        this.customers = customers;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDeveloperSubscriptions() {
        if (developerOrgCode == null || developerOrgCode.isBlank()) return;
        Organization org = organizations.findByOrgCode(developerOrgCode.trim()).orElse(null);
        if (org == null) {
            log.info("개발사 org(code={}) 미존재 — 무상 구독 skip", developerOrgCode);
            return;
        }
        // 구독의 소유 고객: org의 owner 멤버(없으면 아무 멤버). 소유 스코프는 org로 설정한다.
        List<Membership> members = memberships.findByOrganizationId(org.getId());
        Long custId = members.stream().filter(m -> "owner".equals(m.getRole()))
                .map(Membership::getCustomerId).findFirst()
                .or(() -> members.stream().map(Membership::getCustomerId).findFirst())
                .orElse(null);
        if (custId == null) {
            log.info("개발사 org(code={}) 멤버 없음 — 무상 구독 skip", developerOrgCode);
            return;
        }
        Customer cust = customers.findById(custId).orElse(null);
        if (cust == null) return;

        Set<Long> covered = subscriptions
                .findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc("organization", org.getId()).stream()
                .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
                .map(s -> s.getPlan().getProduct().getId())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(DtoMapper.KST);
        int created = 0;
        for (Product p : products.findByStatusInOrderBySortOrderAsc(List.of("active", "coming_soon"))) {
            if (covered.contains(p.getId())) continue;
            Plan top = plans.findFirstByProductIdOrderByAmountDesc(p.getId()).orElse(null);
            if (top == null) continue;
            LocalDate next = "yearly".equals(top.getBillingCycle()) ? today.plusYears(1) : today.plusMonths(1);
            Subscription sub = Subscription.complimentary(cust, top, Instant.now(), next);
            sub.setOwner("organization", org.getId());
            subscriptions.save(sub);
            created++;
            log.info("개발사 무상 구독 생성: 제품={} 플랜={}", p.getName(), top.getName());
        }
        if (created > 0) log.info("개발사(code={}) 무상 구독 {}건 생성 완료", developerOrgCode, created);
    }
}
