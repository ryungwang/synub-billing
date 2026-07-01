package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Payment;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.*;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final CurrentUser currentUser;
    private final CurrentScope scope;
    private final DtoMapper mapper;

    public DashboardService(SubscriptionRepository subscriptions, PaymentRepository payments,
                            CurrentUser currentUser, CurrentScope scope, DtoMapper mapper) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.currentUser = currentUser;
        this.scope = scope;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardDto get() {
        // 현재 소유 스코프(개인/조직)로 조회. 조직이면 멤버십 검증됨.
        Owner owner = scope.readOwner();
        List<Subscription> subs =
                subscriptions.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(owner.type(), owner.id());
        List<Payment> pays =
                payments.findByOwnerOrderByEffectiveDateDesc(owner.type(), owner.id());

        List<SubscriptionDto> active = subs.stream()
                .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
                .map(mapper::toSubscription)
                .toList();

        List<PaymentDto> recent = pays.stream().limit(5).map(mapper::toPayment).toList();

        int activeCount = (int) subs.stream().filter(s -> "active".equals(s.getStatus())).count();
        long monthlyTotal = subs.stream()
                .filter(s -> "active".equals(s.getStatus()) && "monthly".equals(s.getPlan().getBillingCycle()))
                .mapToLong(Subscription::chargeAmount).sum();
        long savedByYearly = subs.stream()
                .filter(s -> "active".equals(s.getStatus()) && "yearly".equals(s.getPlan().getBillingCycle()))
                .mapToLong(s -> Math.round(s.chargeAmount() / 10.0) * 2).sum();
        long paidThisYear = pays.stream()
                .filter(p -> "paid".equals(p.getStatus()))
                .mapToLong(Payment::getAmount).sum();

        Subscription nextBilling = subs.stream()
                .filter(s -> "active".equals(s.getStatus()))
                .min(Comparator.comparing(Subscription::getNextBillingDate))
                .orElse(null);

        SummaryDto summary = new SummaryDto(
                activeCount, monthlyTotal, savedByYearly, paidThisYear,
                nextBilling != null ? nextBilling.getNextBillingDate() : null,
                nextBilling != null ? nextBilling.getPlan().getProduct().getName() : null);

        return new DashboardDto(summary, active, recent, spendHistory(pays));
    }

    /** 최근 6개월 결제완료 합계. */
    private List<SpendPointDto> spendHistory(List<Payment> pays) {
        YearMonth current = YearMonth.from(LocalDate.now(DtoMapper.KST));
        List<SpendPointDto> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            long sum = pays.stream()
                    .filter(p -> "paid".equals(p.getStatus()) && p.getPaidAt() != null)
                    .filter(p -> YearMonth.from(p.getPaidAt().atZone(DtoMapper.KST)).equals(ym))
                    .mapToLong(Payment::getAmount).sum();
            result.add(new SpendPointDto(ym.getMonthValue() + "월", sum));
        }
        return result;
    }
}
