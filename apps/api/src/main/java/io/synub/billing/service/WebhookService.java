package io.synub.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.WebhookDelivery;
import io.synub.billing.repo.WebhookDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 제품 웹훅 발신 (PRD §6.3). HMAC-SHA256 서명 + 지수 백오프 재시도.
 * 비동기(@Async)라 결제 트랜잭션을 막지 않는다 — 웹훅 실패가 결제를 롤백하지 않음(장애 격리).
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookDeliveryRepository deliveries;
    private final AppProperties props;
    private final ObjectMapper json;
    private final HttpClient http;

    public WebhookService(WebhookDeliveryRepository deliveries, AppProperties props,
                          ObjectMapper json) {
        this.deliveries = deliveries;
        this.props = props;
        this.json = json;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.webhook().timeoutMs()))
                .build();
    }

    @Async
    public void send(Long productId, String webhookUrl, String event, Map<String, Object> data) {
        String payload;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", event);
            body.put("timestamp", Instant.now().toString());
            body.put("data", data);
            payload = json.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("웹훅 페이로드 직렬화 실패: {}", e.getMessage());
            return;
        }

        WebhookDelivery delivery = deliveries.save(
                new WebhookDelivery(productId, event, webhookUrl, payload));

        if (webhookUrl == null || webhookUrl.isBlank()) {
            delivery.setStatus("failed");
            delivery.setLastError("등록된 webhook_url 없음");
            deliveries.save(delivery);
            return;
        }

        String signature = sign(payload);
        int maxAttempts = props.webhook().maxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            delivery.setAttempts(attempt);
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .timeout(Duration.ofMillis(props.webhook().timeoutMs()))
                        .header("Content-Type", "application/json")
                        .header("X-Synub-Event", event)
                        .header("X-Synub-Signature", "sha256=" + signature)
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                delivery.setResponseCode(res.statusCode());
                if (res.statusCode() / 100 == 2) {
                    delivery.setStatus("delivered");
                    delivery.setDeliveredAt(Instant.now());
                    deliveries.save(delivery);
                    log.info("웹훅 전송 성공 event={} url={} ({}회)", event, webhookUrl, attempt);
                    return;
                }
                delivery.setLastError("HTTP " + res.statusCode());
            } catch (Exception e) {
                delivery.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            backoff(attempt);
        }

        delivery.setStatus("failed");
        deliveries.save(delivery);
        log.warn("웹훅 전송 실패 event={} url={} ({}회 시도)", event, webhookUrl, maxAttempts);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.webhook().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(2000L, 300L * attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
