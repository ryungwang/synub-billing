package io.synub.billing.service;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.BillingKey;
import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Plan;
import io.synub.billing.domain.Product;
import io.synub.billing.domain.Subscription;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.gateway.PaymentGateway.ChargeResult;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 예약된 플랜 변경(다운그레이드)이 갱신 결제 성공 시에만 실제 반영되는지 검증(C1의 나머지 절반). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingEngineTest {

    @Mock SubscriptionRepository subscriptions;
    @Mock PaymentRepository payments;
    @Mock PaymentGateway gateway;
    @Mock AppProperties props;
    @Mock SubscriptionWebhooks webhooks;
    @Mock BillingNotifier notifier;

    @Mock Subscription sub;
    @Mock Plan pending;
    @Mock Product product;
    @Mock Customer customer;
    @Mock BillingKey billingKey;

    private BillingEngine engine() {
        return new BillingEngine(subscriptions, payments, gateway, props, webhooks, notifier);
    }

    private void wireDueSubscription() {
        when(subscriptions.findDue(anyList(), any())).thenReturn(List.of(sub));
        when(sub.getStatus()).thenReturn("active");
        when(sub.getSeats()).thenReturn(1);
        when(sub.getCreditBalance()).thenReturn(0);
        when(sub.getRetryCount()).thenReturn(0);
        when(sub.getNextBillingDate()).thenReturn(LocalDate.now(DtoMapper.KST));
        when(sub.getBillingKey()).thenReturn(billingKey);
        when(billingKey.getPgBillingKey()).thenReturn("bk_test");
        when(sub.getCustomer()).thenReturn(customer);
        when(customer.getExternalId()).thenReturn("usr_1");
        when(customer.getEmail()).thenReturn("u@synub.io");
        when(pending.getProduct()).thenReturn(product);
        when(pending.getName()).thenReturn("Basic");
        when(pending.getBillingCycle()).thenReturn("monthly");
        when(pending.amountForSeats(1)).thenReturn(5_000);
        when(product.getName()).thenReturn("제품");
    }

    @Test
    void 갱신_결제_성공시_예약된_플랜으로_스왑하고_예약을_비운다() {
        wireDueSubscription();
        when(sub.getPendingPlan()).thenReturn(pending);
        when(gateway.charge(any())).thenReturn(ChargeResult.ok("pg_renew"));

        engine().runDueBilling();

        verify(gateway).charge(any());
        verify(sub).setPlan(pending);       // 예약 반영
        verify(sub).setPendingPlan(null);   // 예약 소진
        verify(webhooks).fire(sub, SubscriptionWebhooks.PLAN_CHANGED);
    }

    @Test
    void 갱신_결제_실패시_예약된_플랜을_반영하지_않는다() {
        wireDueSubscription();
        when(sub.getPendingPlan()).thenReturn(pending);
        when(gateway.charge(any())).thenReturn(new ChargeResult(false, null, "card_declined"));
        when(props.billing()).thenReturn(new AppProperties.Billing(List.of(1, 3, 5)));

        engine().runDueBilling();

        verify(sub, never()).setPlan(any());          // 스왑 안 됨(현재 플랜 유지)
        verify(sub, never()).setPendingPlan(eq(null)); // 예약 유지 → 다음 재시도에 다시 적용
    }

    @Test
    void 해지예약_구독은_결제일에_재청구하지_않고_종료한다() {
        wireDueSubscription();
        when(sub.isCancelAtPeriodEnd()).thenReturn(true); // 해지 예약 상태로 결제일 도래

        engine().runDueBilling();

        verify(gateway, never()).charge(any());                       // 카드 재청구 없음
        verify(sub).setStatus("canceled");                            // 예약 해지 확정(종료)
        verify(webhooks).fire(sub, SubscriptionWebhooks.CANCELED);
    }
}
