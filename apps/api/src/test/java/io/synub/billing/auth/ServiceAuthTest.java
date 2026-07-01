package io.synub.billing.auth;

import io.synub.billing.config.AppProperties;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAuthTest {

    private ServiceAuth auth(String internal, String entKey) {
        AppProperties props = new AppProperties(null, null, null, null, null, null, null, null, null,
                new AppProperties.Internal(internal), new AppProperties.Entitlement(entKey));
        return new ServiceAuth(props);
    }

    @Test
    void 내부시크릿_일치_통과() {
        assertDoesNotThrow(() -> auth("s3cret", "svc").requireInternal("s3cret"));
    }

    @Test
    void 내부시크릿_불일치_거부() {
        assertThrows(ForbiddenException.class, () -> auth("s3cret", "svc").requireInternal("wrong"));
        assertThrows(ForbiddenException.class, () -> auth("s3cret", "svc").requireInternal(null));
    }

    @Test
    void 시크릿_미설정이면_항상_거부_failClosed() {
        assertThrows(ForbiddenException.class, () -> auth("", "svc").requireInternal(""));
        assertThrows(ForbiddenException.class, () -> auth(null, "svc").requireInternal(null));
    }

    @Test
    void 서비스키_일치_통과_불일치_거부() {
        assertDoesNotThrow(() -> auth("s", "svcKey").requireServiceKey("svcKey"));
        assertThrows(ForbiddenException.class, () -> auth("s", "svcKey").requireServiceKey("nope"));
    }
}
