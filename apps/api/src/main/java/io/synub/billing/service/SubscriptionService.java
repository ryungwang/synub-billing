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
import java.time.temporal.ChronoUnit;
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
        Owner owner = scope.readOwner();
        return subscriptions.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(owner.type(), owner.id())
                .stream().map(mapper::toSubscription).toList();
    }

    /** 구독 생성 + 첫 결제 (PRD §7.2). 조직 컨텍스트면 결제 관리 권한 필요. */
    @Transactional
    public SubscriptionDto create(CreateSubscriptionRequest req) {
        Customer me = currentUser.resolve();
        Owner owner = scope.writeOwner();
        Plan plan = plans.findByIdAndProductCompanyId(req.planId(), tenant.companyId())
                .orElseThrow(() -> new NotFoundException("요금제를 찾을 수 없습니다."));
        // 카드는 같은 소유 스코프의 것이어야 한다(개인 카드로 회사 구독 불가, 그 반대도).
        BillingKey key = keys.findByIdAndOwnerTypeAndOwnerId(req.billingKeyId(), owner.type(), owner.id())
                .orElseThrow(() -> new NotFoundException("결제수단을 찾을 수 없습니다."));

        // 인원당 과금이면 좌석 수 반영, 정액이면 1좌석.
        int seats = plan.isPerSeat() ? Math.max(1, req.seats() == null ? 1 : req.seats()) : 1;
        int amount = plan.amountForSeats(seats);

        String paymentId = "synub-" + UUID.randomUUID();
        String orderName = plan.getProduct().getName() + " " + plan.getName() + " 구독";
        PaymentGateway.ChargeResult charge = gateway.charge(new PaymentGateway.ChargeRequest(
                key.getPgBillingKey(), amount, orderName, paymentId,
                me.getExternalId(), me.getEmail(),
                me.phoneForBilling()));
        if (!charge.success()) {
            throw new BadRequestException("첫 결제에 실패했습니다: " + charge.failureReason());
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(DtoMapper.KST);
        LocalDate next = "yearly".equals(plan.getBillingCycle())
                ? today.plusYears(1) : today.plusMonths(1);

        Subscription sub = new Subscription(me, plan, key, "active", now, next);
        sub.setOwner(owner.type(), owner.id());
        sub.setSeats(seats);
        sub.setCancelAtPeriodEnd(false);
        subscriptions.save(sub);

        String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", sub.getId());
        payments.save(new Payment(sub, charge.pgPaymentId(), amount,
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

    /**
     * 좌석 수 변경(인원당 과금) + 비례 정산. 소유 스코프 쓰기 권한 필요.
     * 증가 → 남은 기간 비례분 즉시 청구. 감소 → 비례분을 크레딧으로 적립(다음 청구 차감).
     */
    @Transactional
    public SubscriptionDto changeSeats(Long id, int newSeats) {
        Subscription sub = findOwned(id);
        Plan plan = sub.getPlan();
        if (!plan.isPerSeat()) {
            throw new BadRequestException("인원당 과금 구독이 아닙니다.");
        }
        newSeats = Math.max(1, newSeats);
        int oldSeats = sub.getSeats();
        if (newSeats == oldSeats) {
            return mapper.toSubscription(sub);
        }

        // 남은 기간 비율(오늘~다음청구일 / 현재 청구주기 길이)
        LocalDate today = LocalDate.now(DtoMapper.KST);
        LocalDate next = sub.getNextBillingDate();
        LocalDate cycleStart = "yearly".equals(plan.getBillingCycle())
                ? next.minusYears(1) : next.minusMonths(1);
        long cycleDays = Math.max(1, ChronoUnit.DAYS.between(cycleStart, next));
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(today, next));
        double fraction = Math.min(1.0, (double) remainingDays / cycleDays);

        int seatDelta = newSeats - oldSeats;
        long prorated = Math.round((long) plan.getAmount() * Math.abs(seatDelta) * fraction);

        if (seatDelta > 0 && prorated > 0) {
            // 좌석 추가 — 남은 기간 비례분 즉시 청구
            Customer me = currentUser.resolve();
            String paymentId = "synub-seat" + sub.getId() + "-" + today
                    + "-" + UUID.randomUUID().toString().substring(0, 8);
            String orderName = plan.getProduct().getName() + " 좌석 추가 " + seatDelta + "석(잔여기간 정산)";
            PaymentGateway.ChargeResult charge = gateway.charge(new PaymentGateway.ChargeRequest(
                    sub.getBillingKey().getPgBillingKey(), (int) prorated, orderName, paymentId,
                    me.getExternalId(), me.getEmail(), me.phoneForBilling()));
            if (!charge.success()) {
                throw new BadRequestException("좌석 추가 결제에 실패했습니다: " + charge.failureReason());
            }
            String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-S" + String.format("%04d", sub.getId());
            payments.save(new Payment(sub, charge.pgPaymentId(), (int) prorated,
                    "paid", null, receiptNo, Instant.now()));
        } else if (seatDelta < 0 && prorated > 0) {
            // 좌석 감소 — 남은 기간 비례분을 크레딧으로 적립(다음 청구에서 차감)
            sub.setCreditBalance(sub.getCreditBalance() + (int) prorated);
        }

        sub.setSeats(newSeats);
        return mapper.toSubscription(sub);
    }

    private Subscription findOwned(Long id) {
        // 변경(해지·플랜변경)은 쓰기 → 조직이면 결제 관리 권한 필요.
        Owner owner = scope.writeOwner();
        return subscriptions.findByIdAndOwnerTypeAndOwnerId(id, owner.type(), owner.id())
                .orElseThrow(() -> new NotFoundException("구독을 찾을 수 없습니다."));
    }
}
