package io.synub.sso.crypto;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.synub.sso.config.SsoProperties;
import io.synub.sso.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * RS256 JWT 발급 + JWKS 공개. 개인키로 서명하고, 공개키만 JWKS 로 노출한다.
 * 빌링·타 서비스는 이 JWKS 를 받아 토큰 서명을 검증한다(개인키는 절대 노출되지 않음).
 */
@Component
public class JwtIssuer {

    private static final Logger log = LoggerFactory.getLogger(JwtIssuer.class);

    private final SsoProperties props;
    private final RSAKey signingJwk;   // 개인키 포함(서명용)
    private final RSASSASigner signer;
    private final String kid;

    public JwtIssuer(SsoProperties props) throws Exception {
        this.props = props;
        this.kid = props.jwt().keyId() != null && !props.jwt().keyId().isBlank()
                ? props.jwt().keyId() : UUID.randomUUID().toString();
        this.signingJwk = loadOrGenerate(props.jwt().privateKey(), kid);
        this.signer = new RSASSASigner(signingJwk);
    }

    private RSAKey loadOrGenerate(String pkcs8Base64, String kid) throws Exception {
        if (pkcs8Base64 != null && !pkcs8Base64.isBlank()) {
            byte[] der = Base64.getDecoder().decode(pkcs8Base64.trim());
            RSAPrivateCrtKey priv = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
            RSAPublicKey pub = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent()));
            log.info("SSO 서명키: 설정된 개인키 로드 (kid={})", kid);
            return new RSAKey.Builder(pub).privateKey((RSAPrivateKey) priv)
                    .keyID(kid).algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        log.warn("SSO 서명키 미설정 → 임시 RSA 키 생성(kid={}). 재기동 시 바뀌어 기존 토큰 무효. 운영은 SSO_SIGNING_KEY 주입.", kid);
        return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID(kid).algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
    }

    /** 계정에 대한 액세스 토큰 발급. sub=external_id, iss/aud/exp 는 설정값. */
    public String issue(Account account) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(props.jwt().issuer())
                .subject(account.getExternalId())
                .audience(props.jwt().audiences())
                .claim("email", account.getEmail())
                .claim("name", account.getName())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(props.jwt().ttlSeconds())))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
                claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    public long ttlSeconds() {
        return props.jwt().ttlSeconds();
    }

    /** 공개키만 담은 JWKS(JSON). 개인키는 포함되지 않는다. */
    public Map<String, Object> publicJwks() {
        return new JWKSet(signingJwk.toPublicJWK()).toJSONObject();
    }
}
