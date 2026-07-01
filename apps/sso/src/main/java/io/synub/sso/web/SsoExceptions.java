package io.synub.sso.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** SSO 도메인 예외 + 매핑. 인증 실패는 상세 노출 없이 401 통일(정보 누출 방지). */
public final class SsoExceptions {

    private SsoExceptions() {}

    /** 이미 가입된 이메일. */
    public static class EmailTakenException extends RuntimeException {
        public EmailTakenException(String message) { super(message); }
    }

    /** 로그인 실패(이메일 없음/비번 불일치). 어느 쪽인지 구분해서 알리지 않는다. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() { super("이메일 또는 비밀번호가 올바르지 않습니다."); }
    }

    @RestControllerAdvice
    public static class Handler {

        @ExceptionHandler(EmailTakenException.class)
        public ResponseEntity<Map<String, Object>> onTaken(EmailTakenException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "email_taken", "message", e.getMessage()));
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Map<String, Object>> onInvalid(InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_credentials", "message", e.getMessage()));
        }
    }
}
