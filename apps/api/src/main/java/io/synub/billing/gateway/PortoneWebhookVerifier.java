package io.synub.billing.gateway;

import io.synub.billing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 포트원 V2 웹훅 서명 검증(Standard Webhooks 규격).
 * webhook-id / webhook-timestamp / webhook-signature 헤더로 위조·리플레이를 차단한다.
 * 시크릿(whsec_...) 미설정이면 검증 불가 → 거부(fail-closed). 결제 상태 위조 방지의 핵심.
 */
@Component
public class PortoneWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(PortoneWebhookVerifier.class);
    private static final long TOLERANCE_SEC = 300; // 리플레이 방지(±5분)

    private final AppProperties.Portone cfg;

    public PortoneWebhookVerifier(AppProperties props) {
        this.cfg = props.portone();
    }

    public boolean verify(String rawBody, String webhookId, String webhookTimestamp, String webhookSignature) {
        String secret = cfg.webhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("PortOne webhook-secret 미설정 — 웹훅 거부(fail-closed)");
            return false;
        }
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            return false;
        }
        try {
            long ts = Long.parseLong(webhookTimestamp.trim());
            if (Math.abs(Instant.now().getEpochSecond() - ts) > TOLERANCE_SEC) {
                log.warn("PortOne 웹훅 타임스탬프 관용 초과 — 거부");
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // whsec_<base64> → 실제 키 = base64 decode
        String b64 = secret.startsWith("whsec_") ? secret.substring(6) : secret;
        byte[] key;
        try {
            key = Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            key = secret.getBytes(StandardCharsets.UTF_8);
        }

        String signedContent = webhookId + "." + webhookTimestamp + "." + rawBody;
        String expected = hmacBase64(key, signedContent);

        // webhook-signature: 공백 구분 "v1,<base64sig>" 목록 중 하나라도 일치하면 통과
        for (String part : webhookSignature.split("\\s+")) {
            String sig = part.contains(",") ? part.substring(part.indexOf(',') + 1) : part;
            if (constantTimeEquals(sig, expected)) {
                return true;
            }
        }
        log.warn("PortOne 웹훅 서명 불일치 — 거부");
        return false;
    }

    private String hmacBase64(byte[] key, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("웹훅 서명 계산 실패", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
