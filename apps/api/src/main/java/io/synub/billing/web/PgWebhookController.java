package io.synub.billing.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.domain.Payment;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.gateway.PortoneWebhookVerifier;
import io.synub.billing.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 포트원 → 결제 서비스 PG 웹훅 수신 (PRD §6.4).
 * 서명 검증(Standard Webhooks) 후 {pgPaymentId, status} 로 payment 상태 갱신.
 * (금액 대조는 후속 — PortOne 결제 조회 API로 보강)
 */
@RestController
public class PgWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PgWebhookController.class);

    private final PaymentRepository payments;
    private final PortoneWebhookVerifier verifier;
    private final PaymentGateway gateway;
    private final ObjectMapper json;

    public PgWebhookController(PaymentRepository payments, PortoneWebhookVerifier verifier,
                              PaymentGateway gateway, ObjectMapper json) {
        this.payments = payments;
        this.verifier = verifier;
        this.gateway = gateway;
        this.json = json;
    }

    @PostMapping("/webhooks/portone")
    @Transactional
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature) {

        // 위조·리플레이 차단 — 서명 검증 실패 시 즉시 거부
        if (!verifier.verify(rawBody, webhookId, webhookTimestamp, webhookSignature)) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> body;
        try {
            body = json.readValue(rawBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        String pgPaymentId = str(body.get("pgPaymentId"));
        String pgStatus = str(body.get("status"));
        if (pgPaymentId == null) {
            return ResponseEntity.badRequest().build();
        }

        payments.findByPgPaymentId(pgPaymentId).ifPresentOrElse(p -> {
            String mapped = mapStatus(pgStatus);
            // 금액 대조(PG 조회 지원 시): 우리 기록 금액과 다르면 paid 반영 보류(위조·오류 방어)
            if ("paid".equals(mapped)) {
                var info = gateway.lookup(pgPaymentId);
                if (info.isPresent() && info.get().amount() >= 0
                        && info.get().amount() != p.getAmount()) {
                    log.warn("PG 웹훅 금액 불일치: payment={} local={} pg={} — paid 반영 보류",
                            pgPaymentId, p.getAmount(), info.get().amount());
                    return;
                }
            }
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
