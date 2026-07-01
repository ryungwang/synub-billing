-- 비례 정산용 크레딧 잔액. 좌석 감소 시 남은 기간 비례분을 적립 → 다음 청구에서 차감.
-- (좌석 증가는 즉시 비례 청구하므로 크레딧 불필요.)
ALTER TABLE subscription ADD COLUMN credit_balance INTEGER NOT NULL DEFAULT 0;
