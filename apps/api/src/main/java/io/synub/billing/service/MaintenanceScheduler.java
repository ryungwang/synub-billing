package io.synub.billing.service;

import io.synub.billing.repo.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 유지보수 정리 작업. 오래된 멱등키 삭제(delete는 멱등이라 다중 인스턴스 무해). */
@Component
public class MaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceScheduler.class);

    private final IdempotencyKeyRepository idempotencyKeys;

    public MaintenanceScheduler(IdempotencyKeyRepository idempotencyKeys) {
        this.idempotencyKeys = idempotencyKeys;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeIdempotencyKeys() {
        int deleted = idempotencyKeys.deleteOlderThan(Instant.now().minus(7, ChronoUnit.DAYS));
        if (deleted > 0) log.info("멱등키 정리: {}건 삭제(7일 경과)", deleted);
    }
}
