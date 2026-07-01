-- 구독·카드에 소유주체(owner) 추가 — 개인(customer) 또는 조직(organization).
-- 조직 컨텍스트에서 회사가 실제로 카드 등록·구독할 수 있게 한다. 기존 행은 개인 소유로 백필.
-- customer_id 는 유지(생성자/청구 대상 고객). owner_type/owner_id 가 빌링 소유 스코프.

ALTER TABLE subscription ADD COLUMN owner_type VARCHAR(20);
ALTER TABLE subscription ADD COLUMN owner_id   BIGINT;
UPDATE subscription SET owner_type = 'customer', owner_id = customer_id;
ALTER TABLE subscription ALTER COLUMN owner_type SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN owner_id   SET NOT NULL;

ALTER TABLE billing_key ADD COLUMN owner_type VARCHAR(20);
ALTER TABLE billing_key ADD COLUMN owner_id   BIGINT;
UPDATE billing_key SET owner_type = 'customer', owner_id = customer_id;
ALTER TABLE billing_key ALTER COLUMN owner_type SET NOT NULL;
ALTER TABLE billing_key ALTER COLUMN owner_id   SET NOT NULL;

CREATE INDEX idx_subscription_owner ON subscription(owner_type, owner_id);
CREATE INDEX idx_billing_key_owner  ON billing_key(owner_type, owner_id);
