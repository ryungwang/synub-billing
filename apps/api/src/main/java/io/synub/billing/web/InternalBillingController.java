package io.synub.billing.web;

import io.synub.billing.auth.ServiceAuth;
import io.synub.billing.service.BillingEngine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영/검증용 — 스케줄러를 기다리지 않고 자동청구를 즉시 실행.
 * 공개경로(사용자 토큰 없음)이므로 내부 시크릿(X-Internal-Secret) 헤더로 보호한다.
 */
@RestController
@RequestMapping("/internal/billing")
public class InternalBillingController {

    private final BillingEngine engine;
    private final ServiceAuth serviceAuth;

    public InternalBillingController(BillingEngine engine, ServiceAuth serviceAuth) {
        this.engine = engine;
        this.serviceAuth = serviceAuth;
    }

    @PostMapping("/run")
    public BillingEngine.RunResult run(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        serviceAuth.requireInternal(secret);
        return engine.runDueBilling();
    }
}
