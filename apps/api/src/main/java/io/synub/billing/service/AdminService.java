package io.synub.billing.service;

import io.synub.billing.domain.Organization;
import io.synub.billing.domain.Payment;
import io.synub.billing.domain.Product;
import io.synub.billing.domain.Subscription;
import io.synub.billing.dto.Dtos.AdminOrgDto;
import io.synub.billing.dto.Dtos.AdminPaymentDto;
import io.synub.billing.dto.Dtos.AdminAnalyticsDto;
import io.synub.billing.dto.Dtos.AdminStatsDto;
import io.synub.billing.dto.Dtos.AdminSubscriptionDto;
import io.synub.billing.dto.Dtos.MonthPoint;
import io.synub.billing.dto.Dtos.NameValue;
import io.synub.billing.dto.Dtos.ProductAdminDto;
import io.synub.billing.dto.Dtos.ProductMetaRequest;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.repo.PaymentRepository;
import io.synub.billing.repo.ProductRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final ProductRepository products;

    public AdminService(SubscriptionRepository subscriptions, PaymentRepository payments,
                        CustomerRepository customers, OrganizationRepository organizations,
                        PaymentGateway gateway, StorageService storage, CurrentUser currentUser,
                        ProductRepository products) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.customers = customers;
        this.organizations = organizations;
        this.gateway = gateway;
        this.storage = storage;
        this.currentUser = currentUser;
        this.products = products;
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

    // ---- 제품 메타 관리(관리자) — 가격/플랜은 마이그레이션 전용, 여기선 안 다룸 ----
    @Transactional(readOnly = true)
    public List<ProductAdminDto> products() {
        requireAdmin();
        return products.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toProductDto).toList();
    }

    @Transactional
    public ProductAdminDto createProduct(ProductMetaRequest req) {
        requireAdmin();
        String code = req.serviceCode() == null ? "" : req.serviceCode().trim();
        if (!code.matches("^[a-z0-9-]{2,50}$")) {
            throw new BadRequestException("service_code는 소문자-케밥(a-z,0-9,-) 2~50자여야 합니다.");
        }
        if (products.existsByServiceCode(code)) {
            throw new BadRequestException("이미 존재하는 service_code입니다: " + code);
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("제품명을 입력하세요.");
        }
        Product p = new Product(code, req.name().trim(), blank(req.category()), blank(req.description()),
                blank(req.domainUrl()), blank(req.demoUrl()), blank(req.webhookUrl()), blank(req.onboardingUrl()),
                req.sortOrder() == null ? 0 : req.sortOrder(),
                Boolean.TRUE.equals(req.orgOnly()),
                normalizeStatus(req.status()));
        return toProductDto(products.save(p));
    }

    @Transactional
    public ProductAdminDto updateProduct(Long id, ProductMetaRequest req) {
        requireAdmin();
        Product p = products.findById(id)
                .orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));
        if (req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("제품명을 입력하세요.");
        }
        p.updateMeta(req.name().trim(), blank(req.category()), blank(req.description()),
                blank(req.domainUrl()), blank(req.demoUrl()), blank(req.webhookUrl()), blank(req.onboardingUrl()),
                req.sortOrder() == null ? p.getSortOrder() : req.sortOrder(),
                req.orgOnly() == null ? p.isOrgOnly() : req.orgOnly(),
                normalizeStatus(req.status()));
        return toProductDto(p);
    }

    private ProductAdminDto toProductDto(Product p) {
        return new ProductAdminDto(p.getId(), p.getServiceCode(), p.getName(), p.getCategory(),
                p.getDescription(), p.getDomainUrl(), p.getDemoUrl(), p.getWebhookUrl(),
                p.getOnboardingUrl(), p.getSortOrder(), p.isOrgOnly(), p.getStatus(),
                p.getPlans() == null ? 0 : p.getPlans().size());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** 노출 상태 정규화 — 'inactive'(숨김)만 특별 처리, 그 외 'active'. */
    private static String normalizeStatus(String s) {
        return "inactive".equalsIgnoreCase(s) ? "inactive" : "active";
    }

    // ---- 대시보드 분석(차트 데이터) ----
    @Transactional(readOnly = true)
    public AdminAnalyticsDto analytics() {
        requireAdmin();
        var kst = DtoMapper.KST;
        List<YearMonth> months = new ArrayList<>();
        YearMonth now = YearMonth.now(kst);
        for (int i = 5; i >= 0; i--) months.add(now.minusMonths(i)); // 최근 6개월(오래된→최신)

        List<Payment> pays = payments.findAll();
        List<Subscription> subsList = subscriptions.findAllByOrderByIdDesc();
        List<Organization> orgs = organizations.findAllByOrderByIdDesc();

        // 매출 추세 — paid 결제 월별 합
        Map<YearMonth, Long> revByMonth = new LinkedHashMap<>();
        months.forEach(m -> revByMonth.put(m, 0L));
        for (Payment p : pays) {
            if (!"paid".equals(p.getStatus())) continue;
            Instant when = p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt();
            YearMonth m = YearMonth.from(when.atZone(kst));
            if (revByMonth.containsKey(m)) revByMonth.merge(m, (long) p.getAmount(), Long::sum);
        }
        List<MonthPoint> revenueTrend = months.stream()
                .map(m -> new MonthPoint(m.getMonthValue() + "월", revByMonth.get(m), 0)).toList();

        // 신규 구독 추세 — startedAt 월별 건수
        Map<YearMonth, Long> subsByMonth = new LinkedHashMap<>();
        months.forEach(m -> subsByMonth.put(m, 0L));
        for (Subscription s : subsList) {
            Instant when = s.getStartedAt() != null ? s.getStartedAt() : s.getCreatedAt();
            if (when == null) continue;
            YearMonth m = YearMonth.from(when.atZone(kst));
            if (subsByMonth.containsKey(m)) subsByMonth.merge(m, 1L, Long::sum);
        }
        List<MonthPoint> subsTrend = months.stream()
                .map(m -> new MonthPoint(m.getMonthValue() + "월", 0, subsByMonth.get(m))).toList();

        // 구독 상태 분포(고정 순서)
        List<NameValue> subsByStatus = statusCounts(
                subsList.stream().map(Subscription::getStatus).toList(),
                List.of("active", "past_due", "suspended", "canceled"),
                Map.of("active", "이용중", "past_due", "연체", "suspended", "정지", "canceled", "해지"));

        // 제품별 누적 매출(paid) — 큰 값 순
        Map<String, Long> byProduct = new LinkedHashMap<>();
        for (Payment p : pays) {
            if (!"paid".equals(p.getStatus())) continue;
            String name = p.getSubscription().getPlan().getProduct().getName();
            byProduct.merge(name, (long) p.getAmount(), Long::sum);
        }
        List<NameValue> revenueByProduct = byProduct.entrySet().stream()
                .map(e -> new NameValue(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(NameValue::value).reversed()).toList();

        // 결제 상태 분포
        List<NameValue> paymentsByStatus = statusCounts(
                pays.stream().map(Payment::getStatus).toList(),
                List.of("paid", "failed", "refunded"),
                Map.of("paid", "완료", "failed", "실패", "refunded", "환불"));

        // 회사 인증 상태 분포
        List<NameValue> orgsByStatus = statusCounts(
                orgs.stream().map(Organization::getVerifyStatus).toList(),
                List.of("verified", "pending", "rejected"),
                Map.of("verified", "인증완료", "pending", "심사대기", "rejected", "반려"));

        return new AdminAnalyticsDto(revenueTrend, subsTrend, subsByStatus,
                revenueByProduct, paymentsByStatus, orgsByStatus);
    }

    /** 상태값 목록을 고정 순서·한글 라벨로 카운트. */
    private static List<NameValue> statusCounts(List<String> values, List<String> order, Map<String, String> labels) {
        Map<String, Long> c = new LinkedHashMap<>();
        order.forEach(k -> c.put(k, 0L));
        for (String v : values) if (c.containsKey(v)) c.merge(v, 1L, Long::sum);
        return order.stream().map(k -> new NameValue(labels.getOrDefault(k, k), c.get(k))).toList();
    }
}
