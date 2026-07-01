package io.synub.billing.web;

import io.synub.billing.service.BillingEngine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영/검증용 — 스케줄러를 기다리지 않고 자동청구를 즉시 실행.
 * (실제 운영에선 내부 토큰/네트워크로 보호 필요)
 */
@RestController
@RequestMapping("/internal/billing")
public class InternalBillingController {

    private final BillingEngine engine;

    public InternalBillingController(BillingEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/run")
    public BillingEngine.RunResult run() {
        return engine.runDueBilling();
    }
}
