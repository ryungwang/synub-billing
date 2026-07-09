-- pg_payment_id 는 PG 결제건의 멱등키 겸 웹훅 대조 키(V1 주석). 유니크 제약이 없어
-- 같은 PG 결제건이 두 번 기록될 수 있었다. 부분 유니크 인덱스로 중복을 DB 레벨에서 차단한다.
-- 실패 결제는 pg_payment_id 가 NULL 이므로(다수 존재) NULL 은 제외한 부분 인덱스로 만든다.
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_pg_payment_id
    ON payment (pg_payment_id)
    WHERE pg_payment_id IS NOT NULL;
