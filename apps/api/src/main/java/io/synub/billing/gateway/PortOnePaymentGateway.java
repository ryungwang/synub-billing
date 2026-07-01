package io.synub.billing.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 포트원 V2 실연동 게이트웨이 (PRD §7). 채널은 app.portone.channel-key 로 결정(현재 토스페이먼츠).
 * 빌링키 단건 결제: POST /payments/{paymentId}/billing-key, Authorization: PortOne {API_SECRET}.
 * app.portone.enabled=true 일 때만 활성.
 */
@Component
@ConditionalOnProperty(name = "app.portone.enabled", havingValue = "true")
public class PortOnePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PortOnePaymentGateway.class);

    private final AppProperties.Portone cfg;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public PortOnePaymentGateway(AppProperties props, ObjectMapper json) {
        this.cfg = props.portone();
        this.json = json;
        log.info("포트원 실연동 게이트웨이 활성 (storeId={}, channelKey={}...)",
                cfg.storeId(), abbreviate(cfg.channelKey()));
    }

    @Override
    public ChargeResult charge(ChargeRequest req) {
        String paymentId = req.idempotencyKey();
        String url = cfg.apiBase() + "/payments/"
                + URLEncoder.encode(paymentId, StandardCharsets.UTF_8) + "/billing-key";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("storeId", cfg.storeId());
        body.put("channelKey", cfg.channelKey());
        body.put("billingKey", req.billingKey());
        body.put("orderName", req.orderName());
        body.put("currency", "KRW");
        body.put("amount", Map.of("total", req.amount()));
        Map<String, Object> customer = new LinkedHashMap<>();
        if (req.customerId() != null) customer.put("id", req.customerId());
        if (req.customerEmail() != null) customer.put("email", req.customerEmail());
        if (req.customerPhone() != null) customer.put("phoneNumber", req.customerPhone());
        if (!customer.isEmpty()) body.put("customer", customer);

        try {
            String payload = json.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "PortOne " + cfg.apiSecret())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = res.body() == null || res.body().isBlank()
                    ? null : json.readTree(res.body());

            if (res.statusCode() / 100 == 2) {
                // 성공 응답. status가 있으면 PAID 확인, 없으면 2xx로 성공 처리.
                String status = pickStatus(node);
                if (status == null || "PAID".equalsIgnoreCase(status)) {
                    return ChargeResult.ok(paymentId);
                }
                return ChargeResult.fail("결제 상태=" + status);
            }
            String message = node != null && node.has("message")
                    ? node.get("message").asText() : ("HTTP " + res.statusCode());
            log.warn("포트원 결제 실패: {} ({})", message, res.statusCode());
            return ChargeResult.fail(message);
        } catch (Exception e) {
            log.error("포트원 결제 호출 오류: {}", e.toString());
            return ChargeResult.fail("PG 통신 오류: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(String pgPaymentId, int amount, String reason) {
        if (pgPaymentId == null || pgPaymentId.isBlank()) {
            return RefundResult.fail("PG 결제건 ID 없음");
        }
        String url = cfg.apiBase() + "/payments/"
                + URLEncoder.encode(pgPaymentId, StandardCharsets.UTF_8) + "/cancel";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("storeId", cfg.storeId());
        body.put("reason", reason != null ? reason : "관리자 환불");
        // 전액 환불 시 amount 생략 가능하나 명시(부분환불 대비)
        body.put("amount", amount);
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "PortOne " + cfg.apiSecret())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2) {
                return RefundResult.ok();
            }
            JsonNode node = res.body() == null || res.body().isBlank() ? null : json.readTree(res.body());
            String message = node != null && node.has("message")
                    ? node.get("message").asText() : ("HTTP " + res.statusCode());
            log.warn("포트원 환불 실패: {} ({})", message, res.statusCode());
            return RefundResult.fail(message);
        } catch (Exception e) {
            log.error("포트원 환불 호출 오류: {}", e.toString());
            return RefundResult.fail("PG 통신 오류: " + e.getMessage());
        }
    }

    private static String pickStatus(JsonNode node) {
        if (node == null) return null;
        if (node.has("status")) return node.get("status").asText();
        if (node.has("payment") && node.get("payment").has("status")) {
            return node.get("payment").get("status").asText();
        }
        return null;
    }

    private static String abbreviate(String s) {
        return s == null ? "null" : s.substring(0, Math.min(12, s.length()));
    }
}
