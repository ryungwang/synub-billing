-- 개발사(운영사) 무상 구독 지원: complimentary 플래그 + billing_key 옵셔널(무상은 결제수단 없음).
-- 기동 러너가 개발사 org에 각 제품 최고 플랜으로 comp·active 구독을 보장. 스케줄러는 comp를 청구에서 제외.
ALTER TABLE subscription ADD COLUMN complimentary BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE subscription ALTER COLUMN billing_key_id DROP NOT NULL;
COMMENT ON COLUMN subscription.complimentary IS '무상 구독(개발사 등) — 청구 제외, billing_key 없음';
