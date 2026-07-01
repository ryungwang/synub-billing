package io.synub.billing.service;

import io.synub.billing.domain.Product;
import io.synub.billing.domain.Subscription;
import io.synub.billing.repo.OrganizationRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 구독 상태 변화를 제품 웹훅으로 발신 (PRD §6.3 이벤트).
 * 트랜잭션 내에서 호출 — lazy 연관(plan/product) 접근 후 primitive만 추려 비동기 전송.
 */
@Component
public class SubscriptionWebhooks {

    public static final String ACTIVATED = "subscription.activated";
    public static final String CANCELED = "subscription.canceled";
    public static final String PAYMENT_FAILED = "subscription.payment_failed";
    public static final String SUSPENDED = "subscription.suspended";
    public static final String PLAN_CHANGED = "subscription.plan_changed";

    private final WebhookService webhooks;
    private final OrganizationRepository organizations;

    public SubscriptionWebhooks(WebhookService webhooks, OrganizationRepository organizations) {
        this.webhooks = webhooks;
        this.organizations = organizations;
    }

    public void fire(Subscription sub, String event) {
        Product product = sub.getPlan().getProduct();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subscriptionId", sub.getId());
        data.put("customerExternalId", sub.getCustomer().getExternalId());
        data.put("serviceCode", product.getServiceCode());
        data.put("plan", sub.getPlan().getPlanCode());
        data.put("status", sub.getStatus());
        data.put("amount", sub.getPlan().getAmount());
        data.put("nextBillingDate", String.valueOf(sub.getNextBillingDate()));
        // 조직 소유 구독이면 owner/org_code 포함 → 제품이 조직 테넌트로 그룹핑
        data.put("ownerType", sub.getOwnerType());
        if (Owner.ORGANIZATION.equals(sub.getOwnerType())) {
            organizations.findById(sub.getOwnerId())
                    .ifPresent(o -> data.put("orgCode", o.getOrgCode()));
        }
        webhooks.send(product.getId(), product.getWebhookUrl(), event, data);
    }
}
