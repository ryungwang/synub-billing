package io.synub.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Billing billing,
                            Webhook webhook, Portone portone, Sso sso, Mail mail,
                            Business business, Storage storage,
                            Internal internal, Entitlement entitlement) {

    /** 내부 운영 엔드포인트(/internal) 보호용 시크릿. */
    public record Internal(String secret) {}

    /** 제품→빌링 entitlements 조회 인증용 서비스 API 키. */
    public record Entitlement(String apiKey) {}

    /**
     * 사업자 검증. apiKey 있으면 국세청 API 사용(없으면 형식·체크섬만, 로컬).
     * statusApiUrl=상태조회(계속사업자), validateApiUrl=진위확인(번호+대표자+개업일 일치).
     */
    public record Business(String statusApiUrl, String validateApiUrl, String apiKey) {}

    /** 파일 저장. dir = 로컬 파일시스템 저장 경로(운영은 S3 어댑터로 교체). */
    /** 파일 저장. dir=로컬 파일시스템 경로(dev). s3=운영 버킷 설정. */
    public record Storage(String dir, S3 s3) {}

    /** 운영 S3 저장(사업자등록증 등). bucket/region 필수(prod), prefix로 앱별 네임스페이스. */
    public record S3(String bucket, String region, String prefix) {}

    /**
     * 발신 이메일. from=발신 주소, appBaseUrl=이메일 내 링크 대상(앱 URL).
     * smtpEnabled=true면 실제 SMTP(SES) 발송, false면 로컬 로그 발송(개발).
     */
    public record Mail(String from, String appBaseUrl, boolean smtpEnabled) {}

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
                          String channelKey, String apiSecret, String identityChannelKey,
                          String webhookSecret) {}
}
