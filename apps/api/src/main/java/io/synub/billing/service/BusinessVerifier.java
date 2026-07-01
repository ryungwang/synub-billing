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
import java.util.List;
import java.util.Map;

/**
 * 사업자등록번호 검증. (1) 형식·체크섬(오프라인, 항상) (2) 국세청 상태조회(apiKey 있을 때).
 * 마구잡이 회사 등록을 막는다 — 형식이 틀리거나 폐업/휴업이면 조직 생성 불가.
 */
@Service
public class BusinessVerifier {

    private static final Logger log = LoggerFactory.getLogger(BusinessVerifier.class);
    private static final int[] KEY = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    private final AppProperties.Business cfg;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public BusinessVerifier(AppProperties props, ObjectMapper json) {
        this.cfg = props.business();
        this.json = json;
    }

    /** 숫자만 남긴 10자리 사업자번호. */
    public String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    /** 형식·체크섬 검증(국세청 알고리즘). */
    public boolean isValidFormat(String bizNo) {
        String d = normalize(bizNo);
        if (d.length() != 10) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (d.charAt(i) - '0') * KEY[i];
        sum += ((d.charAt(8) - '0') * 5) / 10;
        int check = (10 - (sum % 10)) % 10;
        return check == (d.charAt(9) - '0');
    }

    /** apiKey(국세청) 설정 여부 — 실서버 검증 가능 여부. */
    public boolean apiEnabled() {
        return cfg.apiKey() != null && !cfg.apiKey().isBlank();
    }

    /**
     * 국세청 진위확인 — 사업자번호 + 대표자명 + 개업일자(YYYYMMDD)가 실제 등록과 일치하는지.
     * apiKey 미설정이면 생략(true). 일치=valid "01".
     */
    public boolean verifyAuthenticity(String bizNo, String openDate, String repName) {
        if (!apiEnabled()) return true;
        String d = normalize(bizNo);
        try {
            String url = cfg.validateApiUrl() + "?serviceKey="
                    + URLEncoder.encode(cfg.apiKey(), StandardCharsets.UTF_8);
            Map<String, Object> biz = Map.of("b_no", d, "start_dt", openDate == null ? "" : openDate,
                    "p_nm", repName == null ? "" : repName);
            String body = json.writeValueAsString(Map.of("businesses", List.of(biz)));
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("사업자 진위확인 HTTP {}", res.statusCode());
                return false; // 진위확인은 실패 시 fail-closed(도용 방지)
            }
            JsonNode data = json.readTree(res.body()).path("data");
            if (data.isArray() && !data.isEmpty()) {
                return "01".equals(data.get(0).path("valid").asText(""));
            }
            return false;
        } catch (Exception e) {
            log.warn("사업자 진위확인 오류: {}", e.getMessage());
            return false;
        }
    }

    /** 국세청 상태조회로 '계속사업자'인지 확인. apiKey 미설정이면 생략(형식검증만으로 통과). */
    public boolean isActiveBusiness(String bizNo) {
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            return true; // 로컬: 상태조회 생략(체크섬만)
        }
        String d = normalize(bizNo);
        try {
            String url = cfg.statusApiUrl() + "?serviceKey="
                    + URLEncoder.encode(cfg.apiKey(), StandardCharsets.UTF_8);
            String body = json.writeValueAsString(Map.of("b_no", List.of(d)));
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("사업자 상태조회 HTTP {} — 거부(fail-closed)", res.statusCode());
                return false; // 키를 켜 둔 이상 장애도 통과시키지 않음(민감 프로젝트)
            }
            JsonNode data = json.readTree(res.body()).path("data");
            if (data.isArray() && !data.isEmpty()) {
                String stat = data.get(0).path("b_stat").asText("");
                String code = data.get(0).path("b_stat_cd").asText("");
                return "계속사업자".equals(stat) || "01".equals(code);
            }
            return false;
        } catch (Exception e) {
            log.warn("사업자 상태조회 오류: {} — 거부(fail-closed)", e.getMessage());
            return false;
        }
    }
}
