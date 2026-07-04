package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.ContextDto;
import io.synub.billing.dto.Dtos.ContextsDto;
import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.dto.Dtos.MySubscriptionDto;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 제품용 entitlement 조회 (PRD §6.2) — 제품이 "이 유저가 이 서비스를, 어떤 플랜으로 이용할 권한이 있는지" 확인.
 *
 * <p><b>컨텍스트 인지(context-aware).</b> 한 사람이 개인 구독과 조직 구독을 동시에 가질 수 있으므로
 * (예: 개인 Basic + 회사 Pro), 제품은 사용자가 현재 어떤 컨텍스트로 서비스를 쓰는지 함께 넘긴다.
 * <ul>
 *   <li>{@code context=null/빈값} — 레거시: 개인 + 모든 소속 조직 중 첫 active(개인 우선). 하위호환용.</li>
 *   <li>{@code context="personal"} — 개인 소유 구독만 판정.</li>
 *   <li>{@code context="org:{orgCode}"} — 해당 조직 소유 구독만 판정. 미소속이면 fail-closed(권한 없음).</li>
 * </ul>
 * 제품은 {@link #listContexts}로 사용자의 컨텍스트 목록(개인 + 소속 조직)을 받아 스위처를 그리고,
 * 선택된 컨텍스트 문자열을 그대로 이 API에 전달한다.
 */
@Service
public class EntitlementService {

    private final CustomerRepository customers;
    private final SubscriptionRepository subscriptions;
    private final MembershipRepository memberships;
    private final OrganizationRepository organizations;
    private final CurrentUser currentUser;

    public EntitlementService(CustomerRepository customers, SubscriptionRepository subscriptions,
                              MembershipRepository memberships, OrganizationRepository organizations,
                              CurrentUser currentUser) {
        this.customers = customers;
        this.subscriptions = subscriptions;
        this.memberships = memberships;
        this.organizations = organizations;
        this.currentUser = currentUser;
    }

    /** 하위호환 오버로드 — 컨텍스트 미지정(개인 + 모든 조직). */
    @Transactional(readOnly = true)
    public EntitlementDto check(String customerExternalId, String serviceCode) {
        return check(customerExternalId, serviceCode, null);
    }

    /** 컨텍스트 인지 entitlement 판정. */
    @Transactional(readOnly = true)
    public EntitlementDto check(String customerExternalId, String serviceCode, String context) {
        Customer customer = resolveCustomer(customerExternalId);
        if (customer == null) return notEntitled();

        return candidatesForContext(customer, serviceCode, context).stream()
                .filter(this::isEntitled)
                .findFirst()
                .map(this::toEntitlement)
                .orElse(notEntitled());
    }

    /** 제품 컨텍스트 스위처의 소스 — 사용자의 개인 + 소속 조직 컨텍스트 목록. */
    @Transactional(readOnly = true)
    public ContextsDto listContexts(String customerExternalId) {
        String externalId = (customerExternalId == null || customerExternalId.isBlank())
                ? currentUser.externalId() : customerExternalId;
        Customer customer = customers.findByExternalId(externalId).orElse(null);

        List<ContextDto> out = new ArrayList<>();
        // 개인 컨텍스트는 항상 존재.
        out.add(new ContextDto("personal", "personal", null, "개인", null));
        if (customer != null) {
            for (Membership m : memberships.findByCustomerId(customer.getId())) {
                Organization org = organizations.findById(m.getOrganizationId()).orElse(null);
                if (org == null) continue;
                out.add(new ContextDto("org", "org:" + org.getOrgCode(),
                        org.getOrgCode(), org.getName(), m.getRole()));
            }
        }
        return new ContextsDto(externalId, out);
    }

    /**
     * 현재 사용자가 전 스코프(개인 + 소속 조직 전체)에서 이용 중인 구독 목록 — 제품 둘러보기 배지용.
     * 현재 컨텍스트와 무관하게 "개인으로 구독 중"·"○○(회사)로 구독 중"을 모두 보여주기 위함.
     * 예약 해지 후 기간이 지난 건은 제외(entitlement와 동일 판정).
     */
    @Transactional(readOnly = true)
    public List<MySubscriptionDto> mine() {
        Customer me = customers.findByExternalId(currentUser.externalId()).orElse(null);
        if (me == null) return List.of();
        List<MySubscriptionDto> out = new ArrayList<>();
        for (Subscription s : subscriptions
                .findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(Owner.CUSTOMER, me.getId())) {
            if (!isEntitled(s)) continue;
            out.add(new MySubscriptionDto(s.getPlan().getProduct().getServiceCode(),
                    s.getPlan().getName(), "personal", null, s.isComplimentary()));
        }
        for (Membership m : memberships.findByCustomerId(me.getId())) {
            Organization org = organizations.findById(m.getOrganizationId()).orElse(null);
            if (org == null) continue;
            for (Subscription s : subscriptions
                    .findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(Owner.ORGANIZATION, org.getId())) {
                if (!isEntitled(s)) continue;
                out.add(new MySubscriptionDto(s.getPlan().getProduct().getServiceCode(),
                        s.getPlan().getName(), "org", org.getName(), s.isComplimentary()));
            }
        }
        return out;
    }

    /** 선택된 컨텍스트에 해당하는 구독 후보. 알 수 없거나 권한 없는 컨텍스트는 빈 목록(fail-closed). */
    private List<Subscription> candidatesForContext(Customer customer, String serviceCode, String context) {
        String ctx = context == null ? "" : context.trim();

        if (ctx.isEmpty()) {
            // 레거시(컨텍스트 미지정) — 개인 + 모든 소속 조직.
            List<Subscription> all = new ArrayList<>(
                    subscriptions.findByOwnerAndService(Owner.CUSTOMER, customer.getId(), serviceCode));
            for (Membership m : memberships.findByCustomerId(customer.getId())) {
                all.addAll(subscriptions.findByOwnerAndService(
                        Owner.ORGANIZATION, m.getOrganizationId(), serviceCode));
            }
            return all;
        }
        if (ctx.equalsIgnoreCase("personal")) {
            return subscriptions.findByOwnerAndService(Owner.CUSTOMER, customer.getId(), serviceCode);
        }
        if (ctx.regionMatches(true, 0, "org:", 0, 4)) {
            String orgCode = ctx.substring(4).trim();
            Organization org = organizations.findByOrgCode(orgCode).orElse(null);
            if (org == null) return List.of();
            // 멤버십 검증(fail-closed) — 그 조직 소속이 아니면 조직 컨텍스트로 이용 불가.
            boolean member = memberships
                    .findByOrganizationIdAndCustomerId(org.getId(), customer.getId()).isPresent();
            if (!member) return List.of();
            return subscriptions.findByOwnerAndService(Owner.ORGANIZATION, org.getId(), serviceCode);
        }
        // 알 수 없는 컨텍스트 형식 → fail-closed.
        return List.of();
    }

    private Customer resolveCustomer(String customerExternalId) {
        String externalId = (customerExternalId == null || customerExternalId.isBlank())
                ? currentUser.externalId() : customerExternalId;
        return customers.findByExternalId(externalId).orElse(null);
    }

    private boolean isEntitled(Subscription s) {
        if (!"active".equals(s.getStatus()) && !"past_due".equals(s.getStatus())) return false;
        // 예약 해지(cancel_at_period_end)면 다음 결제일 경과 시 종료 — 스케줄러 없이 판정 시점에 계산.
        if (s.isCancelAtPeriodEnd() && s.getNextBillingDate() != null
                && LocalDate.now(DtoMapper.KST).isAfter(s.getNextBillingDate())) {
            return false;
        }
        return true;
    }

    private EntitlementDto notEntitled() {
        return new EntitlementDto(false, null, null, List.of(), null);
    }

    private EntitlementDto toEntitlement(Subscription s) {
        // 조직 소유 구독이면 org_code 포함 → 제품이 조직 테넌트로 그룹핑
        String orgCode = null;
        if (Owner.ORGANIZATION.equals(s.getOwnerType())) {
            orgCode = organizations.findById(s.getOwnerId())
                    .map(Organization::getOrgCode).orElse(null);
        }
        return new EntitlementDto(
                "active".equals(s.getStatus()),
                s.getPlan().getPlanCode(),
                s.getNextBillingDate(),
                s.getPlan().getFeatures(),
                orgCode);
    }
}
