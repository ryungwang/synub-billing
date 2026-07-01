package io.synub.billing.service;

import io.synub.billing.domain.Organization;
import io.synub.billing.domain.Payment;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.AdminOrgDto;
import io.synub.billing.dto.Dtos.AdminPaymentDto;
import io.synub.billing.dto.Dtos.AdminStatsDto;
import io.synub.billing.dto.Dtos.AdminSubscriptionDto;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.storage.StorageService;
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

    private static final java.security.SecureRandom RNG = new java.security.SecureRandom();

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final CustomerRepository customers;
    private final OrganizationRepository organizations;
    private final PaymentGateway gateway;
    private final StorageService storage;
    private final CurrentUser currentUser;

    public AdminService(SubscriptionRepository subscriptions, PaymentRepository payments,
                        CustomerRepository customers, OrganizationRepository organizations,
                        PaymentGateway gateway, StorageService storage, CurrentUser currentUser) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.customers = customers;
        this.organizations = organizations;
        this.gateway = gateway;
        this.storage = storage;
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

    // ---- 회사 인증 심사 ----

    @Transactional(readOnly = true)
    public List<AdminOrgDto> organizations() {
        requireAdmin();
        return organizations.findAllByOrderByIdDesc().stream()
                .map(o -> new AdminOrgDto(o.getId(), o.getName(), o.getBusinessNo(), o.getOrgCode(),
                        o.getVerifyStatus(), o.getRejectReason()))
                .toList();
    }

    @Transactional
    public void approveOrganization(Long id) {
        requireAdmin();
        Organization o = org(id);
        if (o.getOrgCode() == null) {
            o.assignOrgCode(generateOrgCode());
        }
        o.approve(Instant.now());
    }

    /** 조직 코드 생성: SH- + 대문자 10자리. 유니크 제약이 최종 방어, 충돌 시 재시도. */
    private String generateOrgCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("SH-");
            for (int i = 0; i < 10; i++) sb.append((char) ('A' + RNG.nextInt(26)));
            String code = sb.toString();
            if (!organizations.existsByOrgCode(code)) return code;
        }
        throw new IllegalStateException("조직 코드 생성 실패(충돌 과다)");
    }

    @Transactional
    public void rejectOrganization(Long id, String reason) {
        requireAdmin();
        org(id).reject(reason != null && !reason.isBlank() ? reason.trim() : "인증 요건 미충족");
    }

    /** 사업자등록증 서류 열람(관리자 심사용). */
    @Transactional(readOnly = true)
    public DocumentContent organizationDocument(Long id) {
        requireAdmin();
        Organization o = org(id);
        if (o.getBusinessDoc() == null) throw new NotFoundException("첨부 서류가 없습니다.");
        return new DocumentContent(storage.load(o.getBusinessDoc()), contentType(o.getBusinessDoc()));
    }

    public record DocumentContent(byte[] content, String contentType) {}

    private Organization org(Long id) {
        return organizations.findById(id)
                .orElseThrow(() -> new NotFoundException("회사를 찾을 수 없습니다."));
    }

    private String contentType(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".pdf")) return "application/pdf";
        if (k.endsWith(".png")) return "image/png";
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
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
