package io.synub.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sso")
public record SsoProperties(Jwt jwt, Demo demo) {

    /**
     * 토큰 발급 설정. issuer/audiences 는 검증측(빌링 등)과 일치해야 한다.
     * privateKey: RSA 개인키(PKCS8 base64). 비면 부팅 시 임시 키 생성(로컬). 운영은 env 주입.
     */
    public record Jwt(String issuer, List<String> audiences, long ttlSeconds,
                      String privateKey, String keyId) {}

    /**
     * 데모 체험 계정. enabled=true면 부팅 시 이 계정을 보장 생성한다.
     * externalId 를 빌링 시드 고객(demo-user)과 일치시켜, 데모 로그인 시 리치 시드 데이터가 보이게 한다.
     * 운영(prod)은 반드시 false.
     */
    public record Demo(boolean enabled, String email, String password,
                       String externalId, String name) {}
}
