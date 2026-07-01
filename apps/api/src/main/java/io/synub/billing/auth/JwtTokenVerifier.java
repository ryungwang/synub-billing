package io.synub.billing.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.synub.billing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 실제 JWT 서명검증기 (nimbus-jose-jwt). 민감 프로젝트라 서명·iss·aud·exp 를 모두 강제한다.
 * <ul>
 *   <li>mode=hs256: 공유 시크릿(HS256) 검증 — SSO 서비스 준비 전 단계에서 사용.</li>
 *   <li>mode=jwks : SSO 공개키(RS256, JWKS endpoint) 검증 — 운영.</li>
 * </ul>
 * 검증기가 구성되지 않았거나(시크릿/JWKS 미설정) 검증 실패 시 {@link AuthException} → 401.
 */
@Component
public class JwtTokenVerifier implements TokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenVerifier.class);

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwtTokenVerifier(AppProperties props) {
        this.processor = build(props.sso());
    }

    private ConfigurableJWTProcessor<SecurityContext> build(AppProperties.Sso c) {
        JWKSource<SecurityContext> keySource;
        JWSAlgorithm alg;
        try {
            if ("jwks".equalsIgnoreCase(c.mode())) {
                if (c.jwksUri() == null || c.jwksUri().isBlank()) {
                    log.warn("SSO mode=jwks 인데 jwks-uri 미설정 — 토큰 검증 비활성(모든 Bearer 토큰 거부).");
                    return null;
                }
                alg = JWSAlgorithm.RS256;
                keySource = JWKSourceBuilder.<SecurityContext>create(URI.create(c.jwksUri()).toURL())
                        .build();
            } else { // hs256
                if (c.jwtSecret() == null || c.jwtSecret().isBlank()) {
                    log.warn("SSO mode=hs256 인데 jwt-secret 미설정 — 토큰 검증 비활성(모든 Bearer 토큰 거부).");
                    return null;
                }
                alg = JWSAlgorithm.HS256;
                keySource = new ImmutableSecret<>(c.jwtSecret().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("SSO 토큰 검증기 초기화 실패: {}", e.getMessage());
            return null;
        }

        DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
        p.setJWSKeySelector(new JWSVerificationKeySelector<>(alg, keySource));

        // iss 정확 대조 + aud 포함 검증 + sub/exp 필수. exp/nbf 는 아래 clock-skew 로 검증.
        JWTClaimsSet exactMatch = new JWTClaimsSet.Builder().issuer(c.issuer()).build();
        DefaultJWTClaimsVerifier<SecurityContext> claims =
                new DefaultJWTClaimsVerifier<>(c.audience(), exactMatch, Set.of("sub", "exp"));
        claims.setMaxClockSkew((int) c.clockSkewSeconds());
        p.setJWTClaimsSetVerifier(claims);
        return p;
    }

    @Override
    public Identity verify(String token) {
        if (processor == null) {
            throw new AuthException("SSO 토큰 검증이 구성되지 않았습니다.");
        }
        try {
            JWTClaimsSet claims = processor.process(token, null);
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new AuthException("토큰에 sub(사용자 식별자)가 없습니다.");
            }
            String email = claims.getStringClaim("email");
            return new Identity(sub, email, AuthContext.personal());
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            // 서명 불일치·만료·iss/aud 불일치 등 모든 검증 실패를 401 로 흡수
            throw new AuthException("토큰 검증 실패: " + e.getMessage());
        }
    }
}
