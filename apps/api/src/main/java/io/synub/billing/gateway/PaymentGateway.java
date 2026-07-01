package io.synub.billing.gateway;

/**
 * PG 결제 게이트웨이 추상화. 실연동(포트원 V2 + KG이니시스)은 PortOnePaymentGateway,
 * 로컬/테스트는 MockPaymentGateway (app.portone.enabled 로 분기). 특정 SDK 직접 의존 금지.
 */
public interface PaymentGateway {

    /** 빌링키로 즉시 청구. */
    ChargeResult charge(ChargeRequest req);

    /** 결제 취소(환불). pgPaymentId 로 PG 결제건을 취소. */
    RefundResult refund(String pgPaymentId, int amount, String reason);

    /**
     * PG 결제건 조회(웹훅 금액 대조용). 지원하지 않으면 empty(서명검증만 신뢰).
     * PortOne 실연동만 조회 가능, Mock 은 empty.
     */
    default java.util.Optional<PaymentInfo> lookup(String pgPaymentId) {
        return java.util.Optional.empty();
    }

    record PaymentInfo(String status, int amount) {}

    record RefundResult(boolean success, String failureReason) {
        public static RefundResult ok() { return new RefundResult(true, null); }
        public static RefundResult fail(String reason) { return new RefundResult(false, reason); }
    }

    /**
     * 청구 요청. KG이니시스는 customer 전화번호·이메일이 필수라 함께 전달.
     * idempotencyKey는 PG 결제건 ID(paymentId)로 사용 — 중복 청구 방지.
     */
    record ChargeRequest(
            String billingKey,
            int amount,
            String orderName,
            String idempotencyKey,
            String customerId,
            String customerEmail,
            String customerPhone) {}

    record ChargeResult(boolean success, String pgPaymentId, String failureReason) {
        public static ChargeResult ok(String pgPaymentId) {
            return new ChargeResult(true, pgPaymentId, null);
        }
        public static ChargeResult fail(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
