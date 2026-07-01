package io.synub.sso.web;

import io.synub.sso.crypto.JwtIssuer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 공개키 배포(JWKS). 빌링·타 서비스가 이 endpoint 로 공개키를 받아 토큰 서명을 검증한다.
 * 개인키는 절대 노출되지 않는다.
 */
@RestController
public class JwksController {

    private final JwtIssuer jwtIssuer;

    public JwksController(JwtIssuer jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtIssuer.publicJwks();
    }
}
