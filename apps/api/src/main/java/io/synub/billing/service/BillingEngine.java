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

    public BillingEngine(SubscriptionRepository subscriptions, PaymentRepository payments,
                         PaymentGateway gateway, AppProperties props,
                         SubscriptionWebhooks webhooks) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.gateway = gateway;
        this.props = props;
        this.webhooks = webhooks;
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
        int amount = sub.chargeAmount(); // 좌석 수 반영(인원당 과금)

        PaymentGateway.ChargeResult result = gateway.charge(new PaymentGateway.ChargeRequest(
                sub.getBillingKey().getPgBillingKey(), amount, orderName, paymentId,
                sub.getCustomer().getExternalId(), sub.getCustomer().getEmail(),
                "010-0000-0000")); // TODO: 고객 전화번호 수집 후 전달

        if (result.success()) {
            String receiptNo = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + String.format("%06d", sub.getId());
            payments.save(new Payment(sub, result.pgPaymentId(), amount,
                    "paid", null, receiptNo, Instant.now()));

            LocalDate base = wasPastDue ? today : sub.getNextBillingDate();
            sub.setNextBillingDate("yearly".equals(plan.getBillingCycle())
                    ? base.plusYears(1) : base.plusMonths(1));
            sub.setStatus("active");
            sub.setRetryCount(0);

            if (wasPastDue) {
                webhooks.fire(sub, SubscriptionWebhooks.ACTIVATED);
                return Outcome.RECOVERED;
            }
            return Outcome.CHARGED;
        }

        // 실패 처리
        payments.save(new Payment(sub, null, amount,
                "failed", result.failureReason(), null, null));
        int attempt = sub.getRetryCount() + 1;
        sub.setRetryCount(attempt);

        if (attempt > props.billing().maxRetries()) {
            sub.setStatus("suspended");
            webhooks.fire(sub, SubscriptionWebhooks.SUSPENDED);
            return Outcome.SUSPENDED;
        }
        sub.setStatus("past_due");
        sub.setNextBillingDate(today.plusDays(props.billing().retryGapDays(attempt)));
        webhooks.fire(sub, SubscriptionWebhooks.PAYMENT_FAILED);
        return Outcome.FAILED;
    }
}
