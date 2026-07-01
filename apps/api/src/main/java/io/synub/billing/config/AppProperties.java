package io.synub.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Tenant tenant, Cors cors, Billing billing,
                            Webhook webhook, Portone portone, Sso sso) {

    public record Tenant(long defaultCompanyId) {}

    /**
     * SSO(통합계정) 연동 — 민감 프로젝트라 실제 JWT 서명검증을 강제한다.
     * mode: "hs256"(공유 시크릿 서명, SSO 준비 전 단계) | "jwks"(SSO 공개키 서명, 운영).
     * jwtSecret: hs256 검증 공유키(운영 env 주입, 디폴트 금지). jwksUri: jwks 모드에서 SSO 공개키 endpoint.
     * issuer/audience: 토큰의 iss/aud를 반드시 대조. clockSkewSeconds: 만료/발급시각 허용 오차.
     * devFallbackEnabled=true면 Bearer 토큰이 없을 때 데모 신원으로 폴백(로컬 전용). 운영은 반드시 false.
     */
    public record Sso(String mode, String jwtSecret, String jwksUri,
                      String issuer, String audience, long clockSkewSeconds,
                      boolean devFallbackEnabled, String devExternalId, String devEmail) {}

    public record Cors(String allowedOrigins) {
        public String[] origins() {
            return allowedOrigins == null || allowedOrigins.isBlank()
                    ? new String[0]
                    : allowedOrigins.split("\\s*,\\s*");
        }
    }

    /** 자동청구·재시도 정책. retryDays = 결제 실패 후 재시도 간격(일). 모두 소진 시 suspended. */
    public record Billing(List<Integer> retryDays) {
        public int maxRetries() {
            return retryDays == null ? 0 : retryDays.size();
        }
        public int retryGapDays(int attempt) {
            // attempt: 1-based. 범위를 넘으면 마지막 간격 사용.
            int idx = Math.min(Math.max(attempt, 1), retryDays.size()) - 1;
            return retryDays.get(idx);
        }
    }

    /** 제품 웹훅 발신 설정. secret으로 HMAC 서명, attempts 만큼 재시도. */
    public record Webhook(String secret, int timeoutMs, int maxAttempts) {}

    /**
     * 포트원 V2 연동. enabled=true면 실연동(PortOnePaymentGateway), false면 Mock.
     * apiSecret은 시크릿이라 디폴트 금지(env 주입). channelKey는 발급 채널, storeId는 상점 식별코드.
     */
    public record Portone(boolean enabled, String apiBase, String storeId,
                          String channelKey, String apiSecret) {}
}
