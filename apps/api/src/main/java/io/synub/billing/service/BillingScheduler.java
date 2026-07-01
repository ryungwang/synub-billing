package io.synub.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동청구 스케줄러 (PRD §7.3). 매일 1회 청구 대상 처리.
 * 분산 환경(다중 인스턴스)에선 중복 실행 방지를 위해 ShedLock 도입 검토.
 */
@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private final BillingEngine engine;

    public BillingScheduler(BillingEngine engine) {
        this.engine = engine;
    }

    @Scheduled(cron = "${billing.scheduler.cron}", zone = "${billing.scheduler.zone}")
    public void runDailyBilling() {
        log.info("[스케줄러] 일일 자동청구 시작");
        engine.runDueBilling();
    }
}
