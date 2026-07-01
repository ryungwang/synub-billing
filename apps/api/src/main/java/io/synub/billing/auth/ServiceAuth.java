package io.synub.billing.auth;

import io.synub.billing.config.AppProperties;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 서비스-투-서비스 인증(사용자 토큰이 아닌 공유 시크릿 기반).
 * /internal(운영) · /api/entitlements(제품→빌링) 같은 공개경로 엔드포인트를 보호한다.
 * 시크릿 미설정이거나 불일치면 fail-closed(403).
 */
@Component
public class ServiceAuth {

    private final AppProperties props;

    public ServiceAuth(AppProperties props) {
        this.props = props;
    }

    public void requireInternal(String provided) {
        if (!matches(props.internal() == null ? null : props.internal().secret(), provided)) {
            throw new ForbiddenException("내부 엔드포인트 인증에 실패했습니다.");
        }
    }

    public void requireServiceKey(String provided) {
        if (!matches(props.entitlement() == null ? null : props.entitlement().apiKey(), provided)) {
            throw new ForbiddenException("서비스 인증에 실패했습니다.");
        }
    }

    /** 상수시간 비교. 시크릿 미설정(blank)이면 항상 실패(fail-closed). */
    private boolean matches(String expected, String provided) {
        if (expected == null || expected.isBlank() || provided == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
