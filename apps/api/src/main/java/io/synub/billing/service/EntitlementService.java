package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
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
    private final CurrentTenant tenant;
    private final CurrentUser currentUser;

    public EntitlementService(CustomerRepository customers, SubscriptionRepository subscriptions,
                              MembershipRepository memberships, CurrentTenant tenant,
                              CurrentUser currentUser) {
        this.customers = customers;
        this.subscriptions = subscriptions;
        this.memberships = memberships;
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
            return new EntitlementDto(false, null, null, List.of());
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
                .orElse(new EntitlementDto(false, null, null, List.of()));
    }

    private EntitlementDto toEntitlement(Subscription s) {
        return new EntitlementDto(
                "active".equals(s.getStatus()),
                s.getPlan().getPlanCode(),
                s.getNextBillingDate(),
                s.getPlan().getFeatures());
    }
}
