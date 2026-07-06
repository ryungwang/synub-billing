package io.synub.billing.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * SSO 신원 주입 필터. Authorization: Bearer <JWT> 를 검증해 {@link IdentityContext} 에 넣는다.
 * Spring Security 를 도입하지 않고(전 엔드포인트 자동잠금 회피) 신원 확립만 담당한다.
 *
 * <p>동작:
 * <ul>
 *   <li>Bearer 토큰 있음 → 서명검증(실패 시 401). X-Synub-Context 로 개인/조직 컨텍스트 결정.</li>
 *   <li>토큰 없음 + dev-fallback(로컬) → 데모 신원. dev-fallback off(운영) → 보호 경로는 401.</li>
 *   <li>공개 경로(/internal,/webhooks,/actuator,/api/entitlements)는 사용자 신원 불필요 → 401 없이 통과.</li>
 * </ul>
 */
@Component
@Order(1)
public class IdentityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdentityFilter.class);

    /** 사용자 신원이 없어도 되는 경로(서버간 호출·웹훅·헬스체크). */
    private static final String[] PUBLIC_PREFIXES = {"/internal", "/webhooks", "/actuator",
            "/api/entitlements", "/api/contexts", "/api/usage", "/api/orgs",
            // 제품 카탈로그(요금)는 마케팅용 공개 정보 — 로그인 전 요금 페이지(/pricing) 서버렌더가 조회.
            "/products",
            // 문의 폼 접수 — 로그인 전 공개 문의 페이지(/contact)에서 제출. 남용 방지는 서비스에서(허니팟·레이트리밋·검증).
            "/inquiries",
            // 아바타 이미지 — 등록된 avatar_key(UUID)만 서빙, <img src>로 로드하므로 공개.
            "/avatars"};

    /** 데모(둘러보기) 계정의 external_id — 이 신원의 변경 요청은 차단(조회 전용). */
    private static final String DEMO_EXTERNAL_ID = "demo-user";

    private final TokenVerifier verifier;
    private final AppProperties.Sso cfg;
    private final ObjectMapper json;

    public IdentityFilter(TokenVerifier verifier, AppProperties props, ObjectMapper json) {
        this.verifier = verifier;
        this.cfg = props.sso();
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // CORS 프리플라이트(OPTIONS)는 인증 대상이 아니다 — 401 처리하면 브라우저 요청이 통째로 막힌다.
        if (CorsUtils.isPreFlightRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean isPublic = isPublicPath(request.getRequestURI());
        String authz = request.getHeader(HttpHeaders.AUTHORIZATION);

        Identity identity = null;
        try {
            if (authz != null && authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authz.substring(7).trim();
                AuthContext context = AuthContext.parse(request.getHeader("X-Synub-Context"));
                identity = verifier.verify(token).withContext(context);
            } else if (cfg.devFallbackEnabled()) {
                AuthContext context = AuthContext.parse(request.getHeader("X-Synub-Context"));
                identity = new Identity(cfg.devExternalId(), cfg.devEmail(), context, false);
            }
        } catch (AuthException e) {
            if (!isPublic) {
                deny(response, e.getMessage());
                return;
            }
            // 공개 경로는 신원 없이 통과 (예: 서비스가 customer 파라미터로 조회)
            log.debug("공개 경로 신원 무시: {}", e.getMessage());
        }

        if (identity == null && !isPublic && !cfg.devFallbackEnabled()) {
            deny(response, "인증이 필요합니다.");
            return;
        }

        // 데모(둘러보기) 계정은 조회 전용 — 운영 데이터·결제 변경을 서버에서 차단(공개 데모의 안전장치).
        if (identity != null && DEMO_EXTERNAL_ID.equals(identity.externalId()) && isMutating(request.getMethod())) {
            forbid(response, "데모 계정은 둘러보기 전용입니다. 구독·결제·변경은 할 수 없습니다.");
            return;
        }

        if (identity != null) {
            IdentityContext.set(identity);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            IdentityContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(),
                Map.of("error", "unauthorized", "message", message));
    }

    private void forbid(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(),
                Map.of("error", "forbidden", "message", message));
    }

    /** 상태를 바꾸는 메서드인지(GET/HEAD/OPTIONS 외). */
    private static boolean isMutating(String method) {
        return !"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
    }
}
