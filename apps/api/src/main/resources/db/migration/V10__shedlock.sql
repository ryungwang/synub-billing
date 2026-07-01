-- 분산 스케줄러 락(ShedLock). 다중 인스턴스에서 자동청구 크론이 한 노드에서만 실행되도록 보장.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
