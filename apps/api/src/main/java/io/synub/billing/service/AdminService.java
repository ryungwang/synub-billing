package io.synub.billing.service;

import io.synub.billing.domain.Payment;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.AdminPaymentDto;
import io.synub.billing.dto.Dtos.AdminStatsDto;
import io.synub.billing.dto.Dtos.AdminSubscriptionDto;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 플랫폼 관리자 콘솔 — 운영자(admin claim) 전용 통계·구독·결제·환불. */
@Service
public class AdminService {

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final CustomerRepository customers;
    private final OrganizationRepository organizations;
    private final PaymentGateway gateway;
    private final CurrentUser currentUser;

    public AdminService(SubscriptionRepository subscriptions, PaymentRepository payments,
                        CustomerRepository customers, OrganizationRepository organizations,
                        PaymentGateway gateway, CurrentUser currentUser) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.customers = customers;
        this.organizations = organizations;
        this.gateway = gateway;
        this.currentUser = currentUser;
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public AdminStatsDto stats() {
        requireAdmin();
        long active = subscriptions.countByStatus("active");
        long mrr = subscriptions.findByStatus("active").stream()
                .mapToLong(s -> "yearly".equals(s.getPlan().getBillingCycle())
                        ? s.chargeAmount() / 12 : s.chargeAmount())
                .sum();
        Instant monthStart = YearMonth.now(DtoMapper.KST)
                .atDay(1).atStartOfDay(DtoMapper.KST).toInstant();
        long paidThisMonth = payments.sumPaidSince(monthStart);
        return new AdminStatsDto(active, customers.count(), organizations.count(), mrr, paidThisMonth);
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionDto> subscriptions() {
        requireAdmin();
        return subscriptions.findAllByOrderByIdDesc().stream()
                .map(this::toAdminSub)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentDto> payments() {
        requireAdmin();
        return payments.findTop100ByOrderByIdDesc().stream()
                .map(this::toAdminPayment)
                .toList();
    }

    @Transactional
    public AdminPaymentDto refund(Long paymentId) {
        requireAdmin();
        Payment p = payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("결제를 찾을 수 없습니다."));
        if (!"paid".equals(p.getStatus())) {
            throw new BadRequestException("결제완료 건만 환불할 수 있습니다.");
        }
        PaymentGateway.RefundResult r = gateway.refund(p.getPgPaymentId(), p.getAmount(), "관리자 환불");
        if (!r.success()) {
            throw new BadRequestException("환불에 실패했습니다: " + r.failureReason());
        }
        p.setStatus("refunded");
        return toAdminPayment(p);
    }

    private AdminSubscriptionDto toAdminSub(Subscription s) {
        var plan = s.getPlan();
        return new AdminSubscriptionDto(s.getId(), s.getCustomer().getEmail(), s.getOwnerType(),
                plan.getProduct().getName(), plan.getName(), s.getStatus(),
                s.chargeAmount(), s.getNextBillingDate());
    }

    private AdminPaymentDto toAdminPayment(Payment p) {
        var sub = p.getSubscription();
        Instant when = p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt();
        String date = LocalDateTime.ofInstant(when, DtoMapper.KST)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new AdminPaymentDto(p.getId(), sub.getCustomer().getEmail(),
                sub.getPlan().getProduct().getName(), p.getAmount(), p.getStatus(),
                date, p.getReceiptNo());
    }
}
