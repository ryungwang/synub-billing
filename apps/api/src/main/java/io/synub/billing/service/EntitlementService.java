package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.tenant.CurrentTenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 제품용 entitlement 조회 (PRD §6.2) — 제품이 "이 유저가 구독 중인지" 확인. */
@Service
public class EntitlementService {

    private final CustomerRepository customers;
    private final SubscriptionRepository subscriptions;
    private final CurrentTenant tenant;
    private final CurrentUser currentUser;

    public EntitlementService(CustomerRepository customers, SubscriptionRepository subscriptions,
                              CurrentTenant tenant, CurrentUser currentUser) {
        this.customers = customers;
        this.subscriptions = subscriptions;
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

        return subscriptions.findByCustomerAndService(customer.getId(), serviceCode).stream()
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
