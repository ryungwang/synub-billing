-- 초대 만료(무기한 pending 방지). 기본 7일.
ALTER TABLE invitation ADD COLUMN expires_at TIMESTAMPTZ;
UPDATE invitation SET expires_at = created_at + interval '7 days' WHERE expires_at IS NULL;
