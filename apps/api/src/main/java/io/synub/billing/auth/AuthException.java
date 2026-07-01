package io.synub.billing.auth;

/** 인증 실패(토큰 검증 실패, 잘못된 컨텍스트 등). 필터에서 401로 매핑된다. */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
