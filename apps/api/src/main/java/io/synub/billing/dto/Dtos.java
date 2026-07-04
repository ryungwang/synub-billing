package io.synub.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** API 입출력 DTO 모음 (프론트 계약과 동일한 JSON 형태). */
public final class Dtos {
    private Dtos() {}

    // ---- 카탈로그 ----
    public record PlanDto(
            Long id, String code, String name, String tagline,
            int amount, String cycle, List<String> features, boolean highlight,
            String pricingType) {}

    public record ProductDto(
            String serviceCode, String name, String category, String description,
            String domain, String demoUrl, long subscribers, boolean orgOnly, String status, List<PlanDto> plans) {}

    // ---- 결제수단 ----
    public record CardDto(
            Long id, String company, String last4, String type,
            boolean isPrimary, long billedCount) {}

    // ---- 구독 ----
    public record UsageDto(String label, String unit, int used, Integer limit) {}

    public record SubscriptionDto(
            Long id, String serviceCode, String product, String plan,
            int amount, String cycle, String status,
            LocalDate startedAt, LocalDate nextBillingDate,
            String card, boolean cancelAtPeriodEnd,
            int monthsActive, UsageDto usage,
            String pricingType, int unitAmount, int seats, int creditBalance,
            boolean complimentary) {}

    // ---- 결제 내역 ----
    public record PaymentDto(
            Long id, String serviceCode, String product, String plan,
            int amount, String status, String date,
            String method, String receiptNo) {}

    // ---- 제품용 entitlement ----
    public record EntitlementDto(
            boolean active, String plan, LocalDate expiresAt, List<String> features, String orgCode) {}

    /**
     * 제품이 컨텍스트 스위처를 그리기 위한 사용자 컨텍스트 1개.
     * type: "personal"(개인) | "org"(조직). 개인이면 orgCode·role null.
     * context: entitlement/스위처에 전달할 값 — 개인="personal", 조직="org:{orgCode}".
     */
    public record ContextDto(String type, String context, String orgCode, String name, String role) {}

    /** 특정 사용자가 가진 컨텍스트 목록(개인 + 소속 조직들). 제품 컨텍스트 스위처의 소스. */
    public record ContextsDto(String customer, List<ContextDto> contexts) {}

    // ---- 대시보드 ----
    public record SpendPointDto(String month, long amount) {}

    public record SummaryDto(
            int activeCount, long monthlyTotal, long savedByYearly,
            long paidThisYear, LocalDate nextBillingDate, String nextBillingProduct) {}

    public record DashboardDto(
            SummaryDto summary,
            List<SubscriptionDto> activeSubscriptions,
            List<PaymentDto> recentPayments,
            List<SpendPointDto> spendHistory) {}

    // ---- 요청 바디 ----
    public record RegisterBillingKeyRequest(
            @NotNull String pgBillingKey, String cardCompany,
            String cardLast4, String cardType, Boolean primary, String phone) {}

    public record CreateSubscriptionRequest(
            @NotNull Long planId, @NotNull Long billingKeyId, Integer seats) {}

    public record ChangePlanRequest(@NotNull Long planId) {}

    /** 제품→빌링 사용량 보고. */
    public record ReportUsageRequest(
            @NotNull String customer, @NotNull String service,
            String label, String unit, int used, Integer limit) {}

    public record ChangeSeatsRequest(@NotNull Integer seats) {}

    // ---- 조직/역할 ----
    /** 내가 속한 조직 + 내 역할 + 인증상태(pending|verified|rejected) + 조직코드. */
    public record OrgDto(Long id, String name, String role, String verifyStatus, String orgCode) {}

    public record CreateOrgRequest(@NotBlank String name) {}

    /** 조직 멤버. */
    public record MemberDto(Long customerId, String externalId, String email, String role) {}

    /** 초대. organizationName 은 내가 받은 초대 조회 시 채워짐. */
    public record InvitationDto(Long id, Long organizationId, String organizationName,
                                String email, String role, String status) {}

    public record CreateInvitationRequest(@NotBlank @Email String email, @NotBlank String role) {}

    public record ChangeRoleRequest(@NotBlank String role) {}

    // ---- 관리자 콘솔 ----
    public record AdminStatsDto(long activeSubscriptions, long customers, long organizations,
                                long monthlyRevenue, long paidThisMonth) {}

    public record AdminSubscriptionDto(Long id, String customerEmail, String ownerType,
                                       String product, String plan, String status,
                                       int amount, LocalDate nextBillingDate) {}

    public record AdminPaymentDto(Long id, String customerEmail, String product,
                                  int amount, String status, String date, String receiptNo) {}

    public record AdminOrgDto(Long id, String name, String businessNo, String orgCode,
                              String verifyStatus, String rejectReason) {}

    /** 관리자 개인(결제 고객) 목록 행 — SSO 통합계정 매핑. subscriptions=이 고객의 총 구독 수. */
    public record AdminCustomerDto(Long id, String externalId, String email, String phone,
                                   long subscriptions, LocalDate createdAt) {}

    /** 마이페이지 프로필 — avatarUrl은 미설정 시 null. */
    public record ProfileDto(String avatarUrl) {}

    public record RejectOrgRequest(String reason) {}

    /** 제품→빌링 조직 멤버 프로비저닝(예: 그룹웨어 직원 추가). role 미지정 시 member. */
    public record ProvisionMemberRequest(
            @NotBlank String orgCode, @NotBlank String externalId,
            String email, String name, String role) {}

    public record OrgMemberDto(String orgCode, String externalId, String role, boolean created) {}

    /** 그룹웨어 초기설정 핸드오프 링크(서명 포함). */
    public record HandoffDto(String url) {}

    // ---- 관리자: 제품 메타 관리(가격/플랜은 마이그레이션 전용) ----
    /** 관리자 콘솔 제품 목록(숨김 포함). 플랜 수만 노출, 가격은 다루지 않음. */
    public record ProductAdminDto(Long id, String serviceCode, String name, String category,
            String description, String domainUrl, String demoUrl, String webhookUrl,
            String onboardingUrl, int sortOrder, boolean orgOnly, String status, int planCount) {}

    /** 제품 메타 등록/수정 입력. serviceCode는 생성 시에만 사용(수정 시 무시). 가격/플랜 필드 없음. */
    public record ProductMetaRequest(String serviceCode, String name, String category,
            String description, String domainUrl, String demoUrl, String webhookUrl,
            String onboardingUrl, Integer sortOrder, Boolean orgOnly, String status) {}

    // ---- 관리자 대시보드 분석(차트) ----
    /** 월별 포인트 — 추세 차트용(매출/건수). */
    public record MonthPoint(String month, long amount, long count) {}
    /** 이름-값 쌍 — 분포(도넛/막대)용. */
    public record NameValue(String name, long value) {}
    /** 관리자 대시보드 차트 데이터 묶음. */
    public record AdminAnalyticsDto(
            List<MonthPoint> revenueTrend,      // 최근 6개월 결제 매출
            List<MonthPoint> subsTrend,         // 최근 6개월 신규 구독 수
            List<NameValue> subsByStatus,       // 구독 상태 분포
            List<NameValue> revenueByProduct,   // 제품별 누적 결제 매출
            List<NameValue> paymentsByStatus,   // 결제 상태 분포
            List<NameValue> orgsByStatus) {}    // 회사 인증 상태 분포
}
