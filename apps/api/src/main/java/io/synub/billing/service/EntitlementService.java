package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.domain.Organization;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.tenant.CurrentTenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 제품용 entitlement 조회 (PRD §6.2) — 제품이 "이 유저가 이 서비스 이용 권한이 있는지" 확인.
 * 권한 = 개인 구독 OR 소속 조직의 구독(둘 중 하나라도 active/past_due).
 */
@Service
public class EntitlementService {

    private final CustomerRepository customers;
    private final SubscriptionRepository subscriptions;
    private final MembershipRepository memberships;
    private final OrganizationRepository organizations;
    private final CurrentTenant tenant;
    private final CurrentUser currentUser;

    public EntitlementService(CustomerRepository customers, SubscriptionRepository subscriptions,
                              MembershipRepository memberships, OrganizationRepository organizations,
                              CurrentTenant tenant, CurrentUser currentUser) {
        this.customers = customers;
        this.subscriptions = subscriptions;
        this.memberships = memberships;
        this.organizations = organizations;
        this.tenant = tenant;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public EntitlementDto check(String customerExternalId, String serviceCode) {
        String externalId = (customerExternalId == null || customerExternalId.isBlank())
                ? currentUser.externalId() : customerExternalId;

        Customer customer = customers
                .findByCompanyIdAndExternalId(tenant.companyId(), externalId)
                .orElse(null);
        if (customer == null) {
            return new EntitlementDto(false, null, null, List.of(), null);
        }

        // 후보 = 개인 소유 구독 + 소속 조직 소유 구독
        List<Subscription> candidates = new ArrayList<>(
                subscriptions.findByOwnerAndService(Owner.CUSTOMER, customer.getId(), serviceCode));
        for (Membership m : memberships.findByCustomerId(customer.getId())) {
            candidates.addAll(subscriptions.findByOwnerAndService(
                    Owner.ORGANIZATION, m.getOrganizationId(), serviceCode));
        }

        return candidates.stream()
                .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
                .findFirst()
                .map(this::toEntitlement)
                .orElse(new EntitlementDto(false, null, null, List.of(), null));
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
