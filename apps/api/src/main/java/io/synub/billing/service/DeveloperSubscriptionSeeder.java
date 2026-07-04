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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 연동 규칙: 개발사(운영사)는 등록된 모든 서비스의 최고 플랜을 무상 구독한다.
 * <p>제품 소유 스코프에 맞춰 무상 구독을 부여한다(멱등 — 이미 active/past_due 구독이 있으면 skip):
 * <ul>
 *   <li><b>조직 전용 제품</b>(org_only=true, 예: 그룹웨어) → 개발사 org(app.developer-org-code) 소유로 구독.
 *       org 멤버 전원이 entitlement 를 상속한다.</li>
 *   <li><b>개인 제품</b>(org_only=false) → 개발자 개인(app.developer-external-ids: haru·sky) 각자 소유로 구독.
 *       개인 제품은 개인 컨텍스트에서 소비되므로 org 소유가 아니라 개발자 개인이 직접 무상 구독한다.</li>
 * </ul>
 * 새 제품이 등록되면 다음 기동 시 소유 스코프에 맞게 자동 구독된다.
 * 무상 구독은 결제수단 없이(billing_key null) active 이며, 스케줄러 청구 대상에서 제외된다(findDue).
 */
@Component
public class DeveloperSubscriptionSeeder {

    private static final Logger log = LoggerFactory.getLogger(DeveloperSubscriptionSeeder.class);

    private static final List<String> SEEDED_STATUSES = List.of("active", "coming_soon");

    @Value("${app.developer-org-code:}")
    private String developerOrgCode;

    /** 개발자 개인 계정(external_id) 목록 — 개인 제품 무상 구독 대상. 쉼표구분. 빈값이면 개인 제품은 skip. */
    @Value("${app.developer-external-ids:}")
    private String developerExternalIds;

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
        List<Product> seeded = products.findByStatusInOrderBySortOrderAsc(SEEDED_STATUSES);
        List<Product> orgProducts = seeded.stream().filter(Product::isOrgOnly).toList();
        List<Product> personalProducts = seeded.stream().filter(p -> !p.isOrgOnly()).toList();

        LocalDate today = LocalDate.now(DtoMapper.KST);
        int created = 0;
        created += ensureOrgSubscriptions(orgProducts, today);
        created += ensureDeveloperPersonalSubscriptions(personalProducts, today);
        if (created > 0) log.info("개발사 무상 구독 {}건 생성 완료", created);
    }

    /** 조직 전용 제품 → 개발사 org 소유 무상 구독. */
    private int ensureOrgSubscriptions(List<Product> orgProducts, LocalDate today) {
        if (orgProducts.isEmpty()) return 0;
        if (developerOrgCode == null || developerOrgCode.isBlank()) return 0;
        Organization org = organizations.findByOrgCode(developerOrgCode.trim()).orElse(null);
        if (org == null) {
            log.info("개발사 org(code={}) 미존재 — 조직제품 무상 구독 skip", developerOrgCode);
            return 0;
        }
        // 구독의 소유 고객: org의 owner 멤버(없으면 아무 멤버). 소유 스코프는 org로 설정한다.
        List<Membership> members = memberships.findByOrganizationId(org.getId());
        Long custId = members.stream().filter(m -> "owner".equals(m.getRole()))
                .map(Membership::getCustomerId).findFirst()
                .or(() -> members.stream().map(Membership::getCustomerId).findFirst())
                .orElse(null);
        Customer cust = custId == null ? null : customers.findById(custId).orElse(null);
        if (cust == null) {
            log.info("개발사 org(code={}) 멤버 없음 — 조직제품 무상 구독 skip", developerOrgCode);
            return 0;
        }

        Set<Long> covered = coveredProductIds(Owner.ORGANIZATION, org.getId());
        int created = 0;
        for (Product p : orgProducts) {
            if (covered.contains(p.getId())) continue;
            Subscription sub = complimentaryTopPlan(cust, p, today);
            if (sub == null) continue;
            sub.setOwner(Owner.ORGANIZATION, org.getId());
            subscriptions.save(sub);
            created++;
            log.info("개발사 org 무상 구독 생성: 제품={} 플랜={}", p.getName(), sub.getPlan().getName());
        }
        return created;
    }

    /** 개인 제품 → 개발자 개인(haru·sky) 각자 소유 무상 구독. */
    private int ensureDeveloperPersonalSubscriptions(List<Product> personalProducts, LocalDate today) {
        if (personalProducts.isEmpty()) return 0;
        List<Customer> devs = developerCustomers();
        if (devs.isEmpty()) {
            log.info("개발자 개인 계정(app.developer-external-ids) 없음 — 개인제품 무상 구독 skip");
            return 0;
        }
        int created = 0;
        for (Customer dev : devs) {
            Set<Long> covered = coveredProductIds(Owner.CUSTOMER, dev.getId());
            for (Product p : personalProducts) {
                if (covered.contains(p.getId())) continue;
                Subscription sub = complimentaryTopPlan(dev, p, today);
                if (sub == null) continue;
                // 개인 소유(owner=customer)로 저장 — complimentary 팩토리 기본값이 개인 소유라 setOwner 불필요.
                subscriptions.save(sub);
                created++;
                log.info("개발자 개인 무상 구독 생성: 개발자={} 제품={} 플랜={}",
                        dev.getExternalId(), p.getName(), sub.getPlan().getName());
            }
        }
        return created;
    }

    /** 설정된 개발자 external_id 를 실제 customer 로 해석(미존재는 제외). */
    private List<Customer> developerCustomers() {
        if (developerExternalIds == null || developerExternalIds.isBlank()) return List.of();
        List<Customer> out = new ArrayList<>();
        for (String extId : Arrays.stream(developerExternalIds.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).distinct().toList()) {
            Customer c = customers.findByExternalId(extId).orElse(null);
            if (c == null) {
                log.info("개발자 개인 계정(external_id={}) 미존재 — skip", extId);
                continue;
            }
            out.add(c);
        }
        return out;
    }

    /** 해당 소유 스코프가 이미 active/past_due 로 구독 중인 제품 id 집합. */
    private Set<Long> coveredProductIds(String ownerType, Long ownerId) {
        return subscriptions.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ownerType, ownerId).stream()
                .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
                .map(s -> s.getPlan().getProduct().getId())
                .collect(Collectors.toSet());
    }

    /** 제품 최고가 플랜으로 무상 구독 생성(플랜 없으면 null). owner 기본값=개인(customer). */
    private Subscription complimentaryTopPlan(Customer customer, Product product, LocalDate today) {
        Plan top = plans.findFirstByProductIdOrderByAmountDesc(product.getId()).orElse(null);
        if (top == null) return null;
        LocalDate next = "yearly".equals(top.getBillingCycle()) ? today.plusYears(1) : today.plusMonths(1);
        return Subscription.complimentary(customer, top, Instant.now(), next);
    }
}
