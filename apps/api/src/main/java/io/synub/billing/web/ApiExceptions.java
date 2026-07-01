package io.synub.billing.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public final class ApiExceptions {
    private ApiExceptions() {}

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    /** 접근 권한 없음(예: 소속되지 않은 조직 컨텍스트). */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    /** 충돌(예: 동일 멱등키 요청이 이미 처리 중). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    @RestControllerAdvice
    public static class Handler {
        @ExceptionHandler(NotFoundException.class)
        public ProblemDetail notFound(NotFoundException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        }

        @ExceptionHandler(BadRequestException.class)
        public ProblemDetail badRequest(BadRequestException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        @ExceptionHandler(ForbiddenException.class)
        public ProblemDetail forbidden(ForbiddenException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        }

        @ExceptionHandler(ConflictException.class)
        public ProblemDetail conflict(ConflictException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
