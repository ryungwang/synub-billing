package io.synub.billing.service;

import io.synub.billing.domain.*;
import io.synub.billing.dto.Dtos.*;
import io.synub.billing.repo.UsageRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class DtoMapper {

    /** 청구일 계산 타임존 — Asia/Seoul (PRD 비기능 요구사항). */
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UsageRepository usageRepo;

    public DtoMapper(UsageRepository usageRepo) {
        this.usageRepo = usageRepo;
    }

    /**
     * 제품이 보고하는 "이번 달 사용량" 스탠드인.
     * 실제로는 각 제품의 usage 보고 API/웹훅으로 채워질 값 — 빌링 도메인 밖이라 임시 매핑.
     */
    private static final Map<String, UsageDto> USAGE = Map.of(
            "doc-analysis", new UsageDto("문서 분석", "건", 642, 2000),
            "threads",      new UsageDto("스레드 생성", "개", 47, 50),
            "office",       new UsageDto("전표 처리", "건", 1820, 5000),
            "desk",         new UsageDto("문의 처리", "건", 312, 1000)
    );

    public PlanDto toPlan(Plan p) {
        return new PlanDto(p.getId(), p.getPlanCode(), p.getName(), p.getTagline(),
                p.getAmount(), p.getBillingCycle(), p.getFeatures(), p.isHighlight(),
                p.getPricingType());
    }

    public ProductDto toProduct(Product p, long subscribers) {
        List<PlanDto> plans = p.getPlans().stream().map(this::toPlan).toList();
        return new ProductDto(p.getServiceCode(), p.getName(), p.getCategory(),
                p.getDescription(), p.getDomainUrl(), subscribers, p.isOrgOnly(), p.getStatus(), plans);
    }

    public CardDto toCard(BillingKey k, long billedCount) {
        return new CardDto(k.getId(), k.getCardCompany(), k.getCardLast4(),
                k.getCardType(), k.isPrimary(), billedCount);
    }

    public SubscriptionDto toSubscription(Subscription s) {
        Plan plan = s.getPlan();
        Product product = plan.getProduct();
        BillingKey key = s.getBillingKey();
        LocalDate started = LocalDate.ofInstant(s.getStartedAt(), KST);
        int months = (int) Math.max(1, ChronoUnit.MONTHS.between(started, LocalDate.now(KST)) + 1);
        String card = key == null ? "무상(개발사)" : key.getCardCompany() + " ····" + key.getCardLast4();
        // 제품이 보고한 실사용량 우선, 없으면 데모 스탠드인 폴백
        UsageDto usage = usageRepo
                .findByExternalIdAndServiceCode(s.getCustomer().getExternalId(), product.getServiceCode())
                .map(u -> new UsageDto(u.getLabel(), u.getUnit(), u.getUsed(), u.getLimitQty()))
                .orElse(USAGE.get(product.getServiceCode()));
        return new SubscriptionDto(
                s.getId(), product.getServiceCode(), product.getName(), plan.getName(),
                s.chargeAmount(), plan.getBillingCycle(), s.getStatus(),
                started, s.getNextBillingDate(), card, s.isCancelAtPeriodEnd(),
                months, usage, plan.getPricingType(), plan.getAmount(), s.getSeats(),
                s.getCreditBalance());
    }

    public PaymentDto toPayment(Payment pay) {
        Subscription s = pay.getSubscription();
        Plan plan = s.getPlan();
        Product product = plan.getProduct();
        var when = pay.getPaidAt() != null ? pay.getPaidAt() : pay.getCreatedAt();
        String date = LocalDateTime.ofInstant(when, KST)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        BillingKey payKey = s.getBillingKey();
        String method = payKey == null ? "무상" : payKey.getCardCompany() + " ····" + payKey.getCardLast4();
        return new PaymentDto(pay.getId(), product.getServiceCode(), product.getName(),
                plan.getName(), pay.getAmount(), pay.getStatus(), date, method,
                pay.getReceiptNo() == null ? "—" : pay.getReceiptNo());
    }
}
