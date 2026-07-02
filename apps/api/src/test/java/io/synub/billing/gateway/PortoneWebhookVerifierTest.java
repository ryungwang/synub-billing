package io.synub.billing.gateway;

import io.synub.billing.config.AppProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PortoneWebhookVerifierTest {

    private static final String SECRET = "whsec_dGVzdGtleQ=="; // key = "testkey"

    private PortoneWebhookVerifier verifier(String secret) {
        AppProperties props = new AppProperties(null, null, null,
                new AppProperties.Portone(false, null, null, null, null, null, secret),
                null, null, null, null, null, null);
        return new PortoneWebhookVerifier(props);
    }

    private String sign(String id, String ts, String body) throws Exception {
        byte[] key = Base64.getDecoder().decode("dGVzdGtleQ==");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
                mac.doFinal((id + "." + ts + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void 올바른_서명_통과() throws Exception {
        String id = "msg_1", ts = String.valueOf(Instant.now().getEpochSecond()), body = "{\"x\":1}";
        assertTrue(verifier(SECRET).verify(body, id, ts, "v1," + sign(id, ts, body)));
    }

    @Test
    void 위조_서명_거부() throws Exception {
        String id = "msg_1", ts = String.valueOf(Instant.now().getEpochSecond()), body = "{\"x\":1}";
        assertFalse(verifier(SECRET).verify(body, id, ts, "v1,forged"));
    }

    @Test
    void 시크릿_없으면_거부_failClosed() throws Exception {
        String id = "msg_1", ts = String.valueOf(Instant.now().getEpochSecond()), body = "{\"x\":1}";
        assertFalse(verifier(null).verify(body, id, ts, "v1," + sign(id, ts, body)));
    }

    @Test
    void 오래된_타임스탬프_리플레이_거부() throws Exception {
        String id = "msg_1", oldTs = String.valueOf(Instant.now().getEpochSecond() - 1000), body = "{\"x\":1}";
        assertFalse(verifier(SECRET).verify(body, id, oldTs, "v1," + sign(id, oldTs, body)));
    }
}
