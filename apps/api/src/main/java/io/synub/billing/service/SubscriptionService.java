package io.synub.billing.service;

import io.synub.billing.domain.*;
import io.synub.billing.dto.Dtos.*;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.*;
import io.synub.billing.tenant.CurrentTenant;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final BillingKeyRepository keys;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final CurrentUser currentUser;
    private final CurrentScope scope;
    private final CurrentTenant tenant;
    private final DtoMapper mapper;
    private final SubscriptionWebhooks webhooks;

    public SubscriptionService(SubscriptionRepository subscriptions, PlanRepository plans,
                               BillingKeyRepository keys, PaymentRepository payments,
                               PaymentGateway gateway, CurrentUser currentUser, CurrentScope scope,
                               CurrentTenant tenant, DtoMapper mapper,
                               SubscriptionWebhooks webhooks) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.keys = keys;
        this.payments = payments;
        this.gateway = gateway;
        this.currentUser = currentUser;
        this.scope = scope;
        this.tenant = tenant;
        this.mapper = mapper;
        this.webhooks = webhooks;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> list() {
        // 조직 컨텍스트: 멤버십 검증 후 빈 목록(조직 소유 구독은 다음 마일스톤)
        if (scope.enforceOrgContext() != null) return List.of();
        Customer me = currentUser.resolve();
        return subscriptions.findByCustomerIdOrderByCreatedAtAsc(me.getId())
                .stream().map(mapper::toSubscription).toList();
    }

    /** 구독 생성 + 첫 결제 (PRD §7.2). */
    @Transactional
    public SubscriptionDto create(CreateSubscriptionRequest req) {
        Customer me = currentUser.resolve();
        Plan plan = plans.findByIdAndProductCompanyId(req.planId(), tenant.companyId())
                .orElseThrow(() -> new NotFoundException("요금제를 찾을 수 없습니다."));
        BillingKey key = keys.findByIdAndCustomerId(req.billingKeyId(), me.getId())
                .orElseThrow(() -> new NotFoundException("결제수단을 찾을 수 없습니다."));

        String paymentId = "synub-" + UUID.randomUUID();
        String orderName = plan.getProduct().getName() + " " + plan.getName() + " 구독";
        PaymentGateway.ChargeResult charge = gateway.charge(new PaymentGateway.ChargeRequest(
                key.getPgBillingKey(), plan.getAmount(), orderName, paymentId,
                me.getExternalId(), me.getEmail(),
                "010-0000-0000")); // TODO: 빌링키 발급 시 고객 전화번호 수집 (KG이니시스 필수)
        if (!charge.success()) {
            throw new BadRequestException("첫 결제에 실패했습니다: " + charge.failureReason());
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(DtoMapper.KST);
        LocalDate next = "yearly".equals(plan.getBillingCycle())
                ? today.plusYears(1) : today.plusMonths(1);

        Subscription sub = new Subscription(me, plan, key, "active", now, next);
        sub.setCancelAtPeriodEnd(false);
        subscriptions.save(sub);

        String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", sub.getId());
        payments.save(new Payment(sub, charge.pgPaymentId(), plan.getAmount(),
                "paid", null, receiptNo, now));

        webhooks.fire(sub, SubscriptionWebhooks.ACTIVATED);
        return mapper.toSubscription(sub);
    }

    /** 해지 — 기간 만료 시 종료(권장). (PRD §3.4) */
    @Transactional
    public SubscriptionDto cancel(Long id) {
        Subscription sub = findOwned(id);
        if ("canceled".equals(sub.getStatus())) {
            throw new BadRequestException("이미 해지된 구독입니다.");
        }
        sub.setStatus("canceled");
        sub.setCanceledAt(Instant.now());
        sub.setCancelAtPeriodEnd(true);
        webhooks.fire(sub, SubscriptionWebhooks.CANCELED);
        return mapper.toSubscription(sub);
    }

    /** 플랜 변경 — MVP는 다음 주기부터 적용(레코드만 갱신). (PRD §3.5) */
    @Transactional
    public SubscriptionDto changePlan(Long id, ChangePlanRequest req) {
        Subscription sub = findOwned(id);
        Plan newPlan = plans.findByIdAndProductCompanyId(req.planId(), tenant.companyId())
                .orElseThrow(() -> new NotFoundException("요금제를 찾을 수 없습니다."));
        sub.setPlan(newPlan);
        webhooks.fire(sub, SubscriptionWebhooks.PLAN_CHANGED);
        return mapper.toSubscription(sub);
    }

    private Subscription findOwned(Long id) {
        Customer me = currentUser.resolve();
        return subscriptions.findByIdAndCustomerId(id, me.getId())
                .orElseThrow(() -> new NotFoundException("구독을 찾을 수 없습니다."));
    }
}
