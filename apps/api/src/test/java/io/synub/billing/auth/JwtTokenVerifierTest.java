package io.synub.billing.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.synub.billing.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SSO 토큰 서명검증기 — 유효/위변조/만료/발급자·대상 불일치/sub 누락 경로를 못박는다. */
class JwtTokenVerifierTest {

    private static final String SECRET = "unit-test-sso-signing-secret-0123456789-abcd"; // 32바이트↑
    private static final String ISS = "https://accounts.synub.io";
    private static final String AUD = "synub-billing";

    private static AppProperties.Sso cfg() {
        return new AppProperties.Sso("hs256", SECRET, null, ISS, AUD, 60,
                false, null, null);
    }

    private static JwtTokenVerifier verifier() {
        return new JwtTokenVerifier(new AppProperties(
                null, null, null, null, cfg(), null, null, null, null, null));
    }

    private static JWTClaimsSet.Builder validClaims() {
        long now = System.currentTimeMillis();
        return new JWTClaimsSet.Builder()
                .subject("sso-user-123")
                .issuer(ISS)
                .audience(AUD)
                .claim("email", "user@synub.io")
                .issueTime(new Date(now))
                .expirationTime(new Date(now + 300_000)); // +5분
    }

    private static String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes()));
        return jwt.serialize();
    }

    @Test
    void 유효한_토큰이면_신원을_복원한다() throws Exception {
        Identity id = verifier().verify(sign(validClaims().build()));
        assertThat(id.externalId()).isEqualTo("sso-user-123");
        assertThat(id.email()).isEqualTo("user@synub.io");
    }

    @Test
    void 서명이_위변조되면_거부한다() throws Exception {
        String token = sign(validClaims().build());
        // 시그니처 마지막 글자를 바꿔 위변조
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');
        assertThatThrownBy(() -> verifier().verify(tampered)).isInstanceOf(AuthException.class);
    }

    @Test
    void 만료된_토큰은_거부한다() throws Exception {
        long past = System.currentTimeMillis() - 3_600_000;
        String token = sign(validClaims()
                .issueTime(new Date(past))
                .expirationTime(new Date(past + 1000)) // 훨씬 과거에 만료
                .build());
        assertThatThrownBy(() -> verifier().verify(token)).isInstanceOf(AuthException.class);
    }

    @Test
    void 발급자가_다르면_거부한다() throws Exception {
        String token = sign(validClaims().issuer("https://evil.example.com").build());
        assertThatThrownBy(() -> verifier().verify(token)).isInstanceOf(AuthException.class);
    }

    @Test
    void 대상_audience가_다르면_거부한다() throws Exception {
        String token = sign(validClaims().audience(List.of("someone-else")).build());
        assertThatThrownBy(() -> verifier().verify(token)).isInstanceOf(AuthException.class);
    }

    @Test
    void sub가_없으면_거부한다() throws Exception {
        String token = sign(validClaims().subject(null).build());
        assertThatThrownBy(() -> verifier().verify(token)).isInstanceOf(AuthException.class);
    }

    @Test
    void 다른_시크릿으로_서명하면_거부한다() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), validClaims().build());
        jwt.sign(new MACSigner("completely-different-secret-0123456789-xyzzy".getBytes()));
        assertThatThrownBy(() -> verifier().verify(jwt.serialize())).isInstanceOf(AuthException.class);
    }
}
