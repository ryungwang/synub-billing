package io.synub.billing.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동청구 스케줄러 (PRD §7.3). 매일 1회 청구 대상 처리.
 * ShedLock 으로 다중 인스턴스에서 한 노드만 실행(중복 청구 방지).
 */
@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private final BillingEngine engine;

    public BillingScheduler(BillingEngine engine) {
        this.engine = engine;
    }

    @Scheduled(cron = "${billing.scheduler.cron}", zone = "${billing.scheduler.zone}")
    @SchedulerLock(name = "billing-daily-run", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void runDailyBilling() {
        log.info("[스케줄러] 일일 자동청구 시작");
        engine.runDueBilling();
    }
}
