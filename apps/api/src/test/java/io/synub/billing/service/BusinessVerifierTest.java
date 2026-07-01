package io.synub.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessVerifierTest {

    private BusinessVerifier verifier() {
        AppProperties props = new AppProperties(null, null, null, null, null, null, null,
                new AppProperties.Business(null, null, null), // apiKey 없음 → 체크섬만
                null, null, null);
        return new BusinessVerifier(props, new ObjectMapper());
    }

    @Test
    void 유효한_사업자번호_체크섬_통과() {
        assertTrue(verifier().isValidFormat("1234567891"));
    }

    @Test
    void 잘못된_체크섬_거부() {
        assertFalse(verifier().isValidFormat("1234567890"));
    }

    @Test
    void 자릿수_틀리면_거부() {
        assertFalse(verifier().isValidFormat("123"));
        assertFalse(verifier().isValidFormat("12345678901"));
    }

    @Test
    void normalize_숫자만_남김() {
        assertEquals("1234567891", verifier().normalize("123-45-67891"));
    }

    @Test
    void 키_없으면_진위확인_상태조회_생략_통과() {
        assertTrue(verifier().verifyAuthenticity("1234567891", "20200101", "김대표"));
        assertTrue(verifier().isActiveBusiness("1234567891"));
        assertFalse(verifier().apiEnabled());
    }
}
