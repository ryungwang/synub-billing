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
            String domain, long subscribers, boolean orgOnly, List<PlanDto> plans) {}

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
            String pricingType, int unitAmount, int seats, int creditBalance) {}

    // ---- 결제 내역 ----
    public record PaymentDto(
            Long id, String serviceCode, String product, String plan,
            int amount, String status, String date,
            String method, String receiptNo) {}

    // ---- 제품용 entitlement ----
    public record EntitlementDto(
            boolean active, String plan, LocalDate expiresAt, List<String> features, String orgCode) {}

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

    public record RejectOrgRequest(String reason) {}

    /** 제품→빌링 조직 멤버 프로비저닝(예: 그룹웨어 직원 추가). role 미지정 시 member. */
    public record ProvisionMemberRequest(
            @NotBlank String orgCode, @NotBlank String externalId,
            String email, String name, String role) {}

    public record OrgMemberDto(String orgCode, String externalId, String role, boolean created) {}
}
