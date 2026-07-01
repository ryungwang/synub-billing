package io.synub.billing.web;

import io.synub.billing.domain.Payment;
import io.synub.billing.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 포트원 → 결제 서비스 PG 웹훅 수신 (PRD §6.4).
 * MVP 스텁: {pgPaymentId, status} 수신 → payment 상태 갱신.
 * 실연동 시 서명 검증 + 포트원 결제 조회 API로 금액/상태 대조 필요.
 */
@RestController
public class PgWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PgWebhookController.class);

    private final PaymentRepository payments;

    public PgWebhookController(PaymentRepository payments) {
        this.payments = payments;
    }

    @PostMapping("/webhooks/portone")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> body) {
        String pgPaymentId = str(body.get("pgPaymentId"));
        String pgStatus = str(body.get("status"));
        if (pgPaymentId == null) {
            return ResponseEntity.badRequest().build();
        }

        payments.findByPgPaymentId(pgPaymentId).ifPresentOrElse(p -> {
            String mapped = mapStatus(pgStatus);
            p.setStatus(mapped);
            if ("paid".equals(mapped) && p.getPaidAt() == null) {
                p.setPaidAt(Instant.now());
            }
            if ("failed".equals(mapped)) {
                p.setFailureReason("PG 통보: " + pgStatus);
            }
            log.info("PG 웹훅 수신: payment={} → {}", pgPaymentId, mapped);
        }, () -> log.warn("PG 웹훅: 알 수 없는 pgPaymentId={}", pgPaymentId));

        return ResponseEntity.ok().build();
    }

    private static String mapStatus(String pg) {
        if (pg == null) return "pending";
        return switch (pg.toLowerCase()) {
            case "paid", "approved", "succeeded" -> "paid";
            case "failed", "declined" -> "failed";
            case "cancelled", "canceled", "refunded" -> "refunded";
            default -> "pending";
        };
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
