package io.synub.billing.service;

import io.synub.billing.domain.*;
import io.synub.billing.dto.Dtos.*;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.*;
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
    private final DtoMapper mapper;
    private final SubscriptionWebhooks webhooks;

    public SubscriptionService(SubscriptionRepository subscriptions, PlanRepository plans,
                               BillingKeyRepository keys, PaymentRepository payments,
                               PaymentGateway gateway, CurrentUser currentUser, CurrentScope scope,
                               DtoMapper mapper,
                               SubscriptionWebhooks webhooks) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.keys = keys;
        this.payments = payments;
        this.gateway = gateway;
        this.currentUser = currentUser;
        this.scope = scope;
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
        Plan plan = plans.findById(req.planId())
                .orElseThrow(() -> new NotFoundException("요금제를 찾을 수 없습니다."));
        // 준비중(coming_soon)·숨김(inactive) 제품은 구독 불가 — 카탈로그 티저로 노출돼도 서버에서 차단.
        if (!"active".equalsIgnoreCase(plan.getProduct().getStatus())) {
            throw new BadRequestException("아직 구독할 수 없는 제품입니다. (출시 준비 중)");
        }
        // 조직 전용 제품(예: 그룹웨어)은 회사(조직) 컨텍스트에서만 구독 가능.
        if (plan.getProduct().isOrgOnly() && !owner.isOrganization()) {
            throw new BadRequestException("이 제품은 회사(조직) 계정만 구독할 수 있습니다. 회사로 전환한 뒤 구독하세요.");
        }
        // 같은 제품 중복 활성 구독 차단 — 이미 이용 중이면 새로 만들지 않는다(이중 청구·상태 오염 방지).
        // 해지·중지된 구독은 재구독 허용(status가 active/past_due 인 것만 in-force로 간주).
        boolean alreadySubscribed = subscriptions
                .findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(owner.type(), owner.id()).stream()
                .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
                .anyMatch(s -> s.getPlan().getProduct().getId().equals(plan.getProduct().getId()));
        if (alreadySubscribed) {
            throw new BadRequestException("이미 이 제품을 구독 중입니다. 플랜 변경으로 조정하세요.");
        }

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

    /**
     * 해지 예약 — 다음 결제일까지 이용, 그 후 자동 종료. (PRD §3.4)
     * status는 active로 유지하고 cancel_at_period_end만 세팅한다. 실제 접근 종료는
     * entitlement가 "예약해지 + 기간경과"로 판정하므로 스케줄러 없이도 다음 결제일에 끊긴다.
     * 즉시 status=canceled로 바꾸지 않으므로 "기간 만료 시 종료" 약속과 동작이 일치한다.
     * 철회({@link #resume})로 되돌릴 수 있다.
     */
    @Transactional
    public SubscriptionDto cancel(Long id) {
        Subscription sub = findOwned(id);
        if (sub.isComplimentary()) {
            throw new BadRequestException("개발사 무상 구독은 해지할 수 없습니다.");
        }
        if ("canceled".equals(sub.getStatus())) {
            throw new BadRequestException("이미 종료된 구독입니다.");
        }
        if (sub.isCancelAtPeriodEnd()) {
            throw new BadRequestException("이미 해지 예약된 구독입니다.");
        }
        sub.setCancelAtPeriodEnd(true);
        sub.setCanceledAt(Instant.now());
        // 웹훅은 발화하지 않는다 — 아직 이용 중(다음 결제일까지)이라 제품이 즉시 차단하면 안 됨.
        // 제품은 entitlement(단일 진실)로 종료 시점을 판정한다.
        return mapper.toSubscription(sub);
    }

    /** 해지 철회 — 예약된 해지를 되돌려 자동 갱신 상태로 복귀. 기간 만료 전에만 가능. */
    @Transactional
    public SubscriptionDto resume(Long id) {
        Subscription sub = findOwned(id);
        if (!sub.isCancelAtPeriodEnd() || sub.isComplimentary()) {
            throw new BadRequestException("해지 예약 상태가 아닙니다.");
        }
        if ("canceled".equals(sub.getStatus())) {
            throw new BadRequestException("이미 종료된 구독은 재개할 수 없습니다. 다시 구독해주세요.");
        }
        if (LocalDate.now(DtoMapper.KST).isAfter(sub.getNextBillingDate())) {
            throw new BadRequestException("이용 기간이 만료되어 재개할 수 없습니다. 다시 구독해주세요.");
        }
        sub.setCancelAtPeriodEnd(false);
        sub.setCanceledAt(null);
        return mapper.toSubscription(sub);
    }

    /**
     * 플랜 변경 (PRD §3.5) — 같은 제품 안에서만 허용. 업/다운 비대칭 처리:
     * <ul>
     *   <li><b>업그레이드</b>(같은 주기·상위금액): 즉시 전환 + 남은 기간 차액 즉시 청구.
     *       (즉시 전환만 하고 청구를 미루면 상위 기능을 무상 이용하는 창이 생기므로 반드시 차액을 받는다.)</li>
     *   <li><b>다운그레이드/결제주기 변경</b>: 다음 결제일에 반영(예약). 지금은 청구·환불·권한변경 없이
     *       현재 플랜을 그대로 이용한다. 반영은 {@link BillingEngine}가 갱신 성공 시 수행한다.</li>
     * </ul>
     * 다른 제품·비활성 제품으로의 변경은 차단(무단 권한 획득 방지).
     */
    @Transactional
    public SubscriptionDto changePlan(Long id, ChangePlanRequest req) {
        Subscription sub = findOwned(id);
        if (sub.isComplimentary()) {
            throw new BadRequestException("개발사 무상 구독은 변경할 수 없습니다.");
        }
        Plan current = sub.getPlan();
        Plan newPlan = plans.findById(req.planId())
                .orElseThrow(() -> new NotFoundException("요금제를 찾을 수 없습니다."));

        // 같은 제품 안에서만 변경 — 다른 제품 플랜으로 갈아타 해당 제품 권한을 무단 획득하는 것 차단.
        if (!newPlan.getProduct().getId().equals(current.getProduct().getId())) {
            throw new BadRequestException("같은 제품의 다른 플랜으로만 변경할 수 있습니다.");
        }
        // 준비중·숨김 제품(플랜)으로는 변경 불가.
        if (!"active".equalsIgnoreCase(newPlan.getProduct().getStatus())) {
            throw new BadRequestException("아직 선택할 수 없는 플랜입니다.");
        }
        if (newPlan.getId().equals(current.getId())) {
            // 현재 플랜과 동일 — 예약된 변경이 있으면 취소하고 현재 상태 반환.
            sub.setPendingPlan(null);
            return mapper.toSubscription(sub);
        }

        int seats = sub.getSeats();
        boolean sameCycle = current.getBillingCycle().equals(newPlan.getBillingCycle());
        int currentAmount = current.amountForSeats(seats);
        int newAmount = newPlan.amountForSeats(seats);

        if (sameCycle && newAmount > currentAmount) {
            // 업그레이드 — 즉시 전환 + 잔여기간 차액 즉시 청구.
            LocalDate today = LocalDate.now(DtoMapper.KST);
            long diff = Math.round((long) (newAmount - currentAmount) * remainingFraction(sub, current));
            if (diff > 0) {
                Customer me = currentUser.resolve();
                String paymentId = "synub-plan" + sub.getId() + "-" + today
                        + "-" + UUID.randomUUID().toString().substring(0, 8);
                String orderName = newPlan.getProduct().getName() + " " + newPlan.getName()
                        + " 업그레이드(잔여기간 차액)";
                PaymentGateway.ChargeResult charge = gateway.charge(new PaymentGateway.ChargeRequest(
                        sub.getBillingKey().getPgBillingKey(), (int) diff, orderName, paymentId,
                        me.getExternalId(), me.getEmail(), me.phoneForBilling()));
                if (!charge.success()) {
                    throw new BadRequestException("업그레이드 결제에 실패했습니다: " + charge.failureReason());
                }
                String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-U" + String.format("%04d", sub.getId());
                payments.save(new Payment(sub, charge.pgPaymentId(), (int) diff,
                        "paid", null, receiptNo, Instant.now()));
            }
            sub.setPlan(newPlan);
            sub.setPendingPlan(null);   // 이전에 예약된 다운그레이드가 있으면 업그레이드가 덮어쓴다.
            webhooks.fire(sub, SubscriptionWebhooks.PLAN_CHANGED);
        } else {
            // 다운그레이드 또는 결제주기 변경 — 다음 결제일에 반영(예약). 지금은 현재 플랜 유지.
            // 웹훅은 실제 반영(갱신) 시점에 발화한다(아직 상위/현재 플랜 이용 중이므로).
            sub.setPendingPlan(newPlan);
        }
        return mapper.toSubscription(sub);
    }

    /** 현재 청구주기에서 오늘 이후 남은 기간의 비율(0~1). 비례정산 공통 계산. */
    private double remainingFraction(Subscription sub, Plan plan) {
        LocalDate today = LocalDate.now(DtoMapper.KST);
        LocalDate next = sub.getNextBillingDate();
        LocalDate cycleStart = "yearly".equals(plan.getBillingCycle())
                ? next.minusYears(1) : next.minusMonths(1);
        long cycleDays = Math.max(1, ChronoUnit.DAYS.between(cycleStart, next));
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(today, next));
        return Math.min(1.0, (double) remainingDays / cycleDays);
    }

    /**
     * 좌석 수 변경(인원당 과금) + 비례 정산. 소유 스코프 쓰기 권한 필요.
     * 증가 → 남은 기간 비례분 즉시 청구. 감소 → 비례분을 크레딧으로 적립(다음 청구 차감).
     */
    @Transactional
    public SubscriptionDto changeSeats(Long id, int newSeats) {
        Subscription sub = findOwned(id);
        if (sub.isComplimentary()) {
            throw new BadRequestException("개발사 무상 구독은 변경할 수 없습니다.");
        }
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
        double fraction = remainingFraction(sub, plan);

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
