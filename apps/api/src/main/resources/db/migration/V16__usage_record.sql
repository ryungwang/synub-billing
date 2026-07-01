-- 제품이 보고하는 이번 청구주기 사용량(하드코딩 스탠드인 대체). (external_id, service_code) 최신값 upsert.
CREATE TABLE usage_record (
    id           BIGSERIAL PRIMARY KEY,
    external_id  VARCHAR(120) NOT NULL,
    service_code VARCHAR(60)  NOT NULL,
    label        VARCHAR(60)  NOT NULL,
    unit         VARCHAR(20)  NOT NULL,
    used         INT          NOT NULL DEFAULT 0,
    limit_qty    INT,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usage UNIQUE (external_id, service_code)
);
