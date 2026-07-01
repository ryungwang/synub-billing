package io.synub.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * 대표자 본인인증 검증(PortOne V2). 프론트에서 통신사 PASS 본인인증 후 받은
 * identityVerificationId 를 서버가 PortOne 에 조회해 인증된 실명을 확인한다.
 * identity 채널·apiSecret 미설정이면 비활성(서류+관리자 심사로 대체).
 */
@Service
public class IdentityVerifier {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerifier.class);

    private final AppProperties.Portone cfg;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public IdentityVerifier(AppProperties props, ObjectMapper json) {
        this.cfg = props.portone();
        this.json = json;
    }

    public boolean enabled() {
        return notBlank(cfg.identityChannelKey()) && notBlank(cfg.apiSecret());
    }

    /** 본인인증 id 조회 → 인증 완료된 실명 반환. 미설정/미인증/오류면 empty. */
    public Optional<String> verifiedName(String identityVerificationId) {
        if (!enabled() || identityVerificationId == null || identityVerificationId.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = cfg.apiBase() + "/identity-verifications/"
                    + URLEncoder.encode(identityVerificationId, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "PortOne " + cfg.apiSecret())
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("본인인증 조회 HTTP {}", res.statusCode());
                return Optional.empty();
            }
            JsonNode node = json.readTree(res.body());
            if (!"VERIFIED".equalsIgnoreCase(node.path("status").asText(""))) {
                return Optional.empty();
            }
            String name = node.path("verifiedCustomer").path("name").asText("");
            return name.isBlank() ? Optional.empty() : Optional.of(name);
        } catch (Exception e) {
            log.warn("본인인증 조회 오류: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
