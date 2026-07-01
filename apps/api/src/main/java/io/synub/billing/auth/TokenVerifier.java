package io.synub.billing.auth;

/** SSO 발급 토큰(JWT)을 검증해 신원을 복원한다. 검증 실패 시 {@link AuthException}. */
public interface TokenVerifier {

    /**
     * 토큰의 서명·발급자(iss)·대상(aud)·만료(exp)를 모두 검증하고 신원을 반환한다.
     * 반환되는 Identity 의 context 는 personal 기본값이며, 컨텍스트는 호출측(필터)이 헤더로 덮어쓴다.
     */
    Identity verify(String token);
}
