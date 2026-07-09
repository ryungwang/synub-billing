package io.synub.billing.service;

import io.synub.billing.domain.BillingKey;
import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Plan;
import io.synub.billing.domain.Product;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.ChangePlanRequest;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.gateway.PaymentGateway.ChargeResult;
import io.synub.billing.repo.BillingKeyRepository;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.PlanRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * changePlan 비대칭 정책(C1) 검증:
 * 업그레이드=즉시 전환+차액 즉시청구 / 다운그레이드·주기변경=다음 결제일 예약(pending) / 타제품=거부.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptions;
    @Mock PlanRepository plans;
    @Mock BillingKeyRepository keys;
    @Mock PaymentRepository payments;
    @Mock PaymentGateway gateway;
    @Mock CurrentUser currentUser;
    @Mock CurrentScope scope;
    @Mock DtoMapper mapper;
    @Mock SubscriptionWebhooks webhooks;

    @Mock Subscription sub;
    @Mock Plan current;
    @Mock Plan target;
    @Mock Product product;      // 현재 구독 제품
    @Mock Product otherProduct; // 다른 제품
    @Mock Customer me;
    @Mock BillingKey billingKey;

    SubscriptionService service;

    private static final long SUB_ID = 10L;
    private static final long TARGET_PLAN_ID = 2L;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptions, plans, keys, payments,
                gateway, currentUser, scope, mapper, webhooks);

        // findOwned: 개인 소유 구독 조회
        when(scope.writeOwner()).thenReturn(Owner.customer(1L));
        when(subscriptions.findByIdAndOwnerTypeAndOwnerId(SUB_ID, "customer", 1L))
                .thenReturn(Optional.of(sub));

        when(sub.isComplimentary()).thenReturn(false);
        when(sub.getPlan()).thenReturn(current);
        when(sub.getSeats()).thenReturn(1);
        when(sub.getNextBillingDate()).thenReturn(LocalDate.now(DtoMapper.KST).plusDays(15));
        when(sub.getBillingKey()).thenReturn(billingKey);
        when(billingKey.getPgBillingKey()).thenReturn("bk_test");

        // 같은 제품, active
        when(current.getId()).thenReturn(1L);
        when(current.getProduct()).thenReturn(product);
        when(current.getBillingCycle()).thenReturn("monthly");
        when(current.amountForSeats(1)).thenReturn(10_000);

        when(target.getId()).thenReturn(TARGET_PLAN_ID);
        when(target.getProduct()).thenReturn(product);
        when(target.getName()).thenReturn("Pro");
        when(target.getBillingCycle()).thenReturn("monthly");
        when(product.getId()).thenReturn(100L);
        when(product.getStatus()).thenReturn("active");
        when(product.getName()).thenReturn("제품");
        when(otherProduct.getId()).thenReturn(200L);
        when(otherProduct.getStatus()).thenReturn("active");

        when(plans.findById(TARGET_PLAN_ID)).thenReturn(Optional.of(target));
        when(currentUser.resolve()).thenReturn(me);
        when(me.getExternalId()).thenReturn("usr_1");
        when(me.getEmail()).thenReturn("u@synub.io");
    }

    @Test
    void 업그레이드는_즉시_전환하고_잔여기간_차액을_청구한다() {
        when(target.amountForSeats(1)).thenReturn(30_000);         // 상위 금액
        when(gateway.charge(any())).thenReturn(ChargeResult.ok("pg_up"));

        service.changePlan(SUB_ID, new ChangePlanRequest(TARGET_PLAN_ID));

        verify(gateway).charge(any());        // 차액 청구됨
        verify(payments).save(any());
        verify(sub).setPlan(target);          // 즉시 전환
        verify(sub).setPendingPlan(null);     // 예약 없음(있던 예약도 해제)
        verify(sub, never()).setPendingPlan(target);
    }

    @Test
    void 다운그레이드는_청구없이_다음_결제일로_예약한다() {
        when(target.amountForSeats(1)).thenReturn(5_000);          // 하위 금액

        service.changePlan(SUB_ID, new ChangePlanRequest(TARGET_PLAN_ID));

        verify(gateway, never()).charge(any());  // 지금 청구 없음
        verify(sub).setPendingPlan(target);      // 예약만
        verify(sub, never()).setPlan(any());     // 현재 플랜 유지(entitlement 불변)
    }

    @Test
    void 결제주기_변경은_상위금액이어도_즉시청구하지_않고_예약한다() {
        when(target.getBillingCycle()).thenReturn("yearly");       // 주기 변경
        when(target.amountForSeats(1)).thenReturn(300_000);        // 연액이 더 큼

        service.changePlan(SUB_ID, new ChangePlanRequest(TARGET_PLAN_ID));

        verify(gateway, never()).charge(any());
        verify(sub).setPendingPlan(target);
        verify(sub, never()).setPlan(any());
    }

    @Test
    void 다른_제품_플랜으로는_변경할_수_없다() {
        when(target.getProduct()).thenReturn(otherProduct);        // 타제품
        when(target.amountForSeats(1)).thenReturn(5_000);

        assertThatThrownBy(() -> service.changePlan(SUB_ID, new ChangePlanRequest(TARGET_PLAN_ID)))
                .isInstanceOf(BadRequestException.class);

        verify(gateway, never()).charge(any());
        verify(sub, never()).setPlan(any());
        verify(sub, never()).setPendingPlan(any());
    }
}
