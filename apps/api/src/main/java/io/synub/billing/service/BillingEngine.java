package io.synub.billing.service;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Payment;
import io.synub.billing.domain.Plan;
import io.synub.billing.domain.Subscription;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 자동청구 엔진 (PRD §7.3/§7.4). 청구 대상 조회 → 빌링키 청구 → 상태머신 전이.
 * active → (실패) → past_due → (재시도 모두 실패) → suspended,  past_due → (성공) → active.
 */
@Service
public class BillingEngine {

    private static final Logger log = LoggerFactory.getLogger(BillingEngine.class);

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final AppProperties props;
    private final SubscriptionWebhooks webhooks;
    private final BillingNotifier notifier;

    public BillingEngine(SubscriptionRepository subscriptions, PaymentRepository payments,
                         PaymentGateway gateway, AppProperties props,
                         SubscriptionWebhooks webhooks, BillingNotifier notifier) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.gateway = gateway;
        this.props = props;
        this.webhooks = webhooks;
        this.notifier = notifier;
    }

    public record RunResult(int processed, int charged, int recovered,
                            int failed, int suspended) {}

    /** 오늘 청구 대상(active/past_due 중 next_billing_date 도래) 일괄 처리. */
    @Transactional
    public RunResult runDueBilling() {
        LocalDate today = LocalDate.now(DtoMapper.KST);
        List<Subscription> due = subscriptions.findDue(List.of("active", "past_due"), today);

        int charged = 0, recovered = 0, failed = 0, suspended = 0;
        for (Subscription sub : due) {
            switch (chargeOne(sub, today)) {
                case CHARGED -> charged++;
                case RECOVERED -> { charged++; recovered++; }
                case FAILED -> failed++;
                case SUSPENDED -> suspended++;
            }
        }
        log.info("자동청구 완료: 대상 {} / 성공 {}(복구 {}) / 실패 {} / 중지 {}",
                due.size(), charged, recovered, failed, suspended);
        return new RunResult(due.size(), charged, recovered, failed, suspended);
    }

    private enum Outcome { CHARGED, RECOVERED, FAILED, SUSPENDED }

    private Outcome chargeOne(Subscription sub, LocalDate today) {
        boolean wasPastDue = "past_due".equals(sub.getStatus());
        Plan plan = sub.getPlan();
        String paymentId = "synub-sub" + sub.getId() + "-" + today
                + "-r" + sub.getRetryCount() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String orderName = plan.getProduct().getName() + " " + plan.getName() + " 정기결제";
        int gross = sub.chargeAmount();                     // 좌석 수 반영 총액
        int credit = sub.getCreditBalance();
        int amount = Math.max(0, gross - credit);           // 크레딧 차감 후 실제 청구액
        int leftoverCredit = Math.max(0, credit - gross);   // 사용 후 남는 크레딧

        boolean success;
        String pgPaymentId = null;
        String failureReason = null;
        if (amount == 0) {
            success = true; // 크레딧으로 전액 충당 — 게이트웨이 미호출
        } else {
            PaymentGateway.ChargeResult result = gateway.charge(new PaymentGateway.ChargeRequest(
                    sub.getBillingKey().getPgBillingKey(), amount, orderName, paymentId,
                    sub.getCustomer().getExternalId(), sub.getCustomer().getEmail(),
                    sub.getCustomer().phoneForBilling()));
            success = result.success();
            pgPaymentId = result.pgPaymentId();
            failureReason = result.failureReason();
        }

        if (success) {
            String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + String.format("%06d", sub.getId());
            payments.save(new Payment(sub, pgPaymentId, amount,
                    "paid", null, receiptNo, Instant.now()));
            sub.setCreditBalance(leftoverCredit); // 크레딧 소비 후 잔액 반영

            LocalDate base = wasPastDue ? today : sub.getNextBillingDate();
            sub.setNextBillingDate("yearly".equals(plan.getBillingCycle())
                    ? base.plusYears(1) : base.plusMonths(1));
            sub.setStatus("active");
            sub.setRetryCount(0);

            if (wasPastDue) {
                webhooks.fire(sub, SubscriptionWebhooks.ACTIVATED);
                notifier.recovered(sub);
                return Outcome.RECOVERED;
            }
            return Outcome.CHARGED;
        }

        // 실패 처리 (크레딧은 소비하지 않음)
        payments.save(new Payment(sub, null, amount,
                "failed", failureReason, null, null));
        int attempt = sub.getRetryCount() + 1;
        sub.setRetryCount(attempt);

        if (attempt > props.billing().maxRetries()) {
            sub.setStatus("suspended");
            webhooks.fire(sub, SubscriptionWebhooks.SUSPENDED);
            notifier.suspended(sub);
            return Outcome.SUSPENDED;
        }
        sub.setStatus("past_due");
        sub.setNextBillingDate(today.plusDays(props.billing().retryGapDays(attempt)));
        webhooks.fire(sub, SubscriptionWebhooks.PAYMENT_FAILED);
        notifier.paymentFailed(sub);
        return Outcome.FAILED;
    }
}
