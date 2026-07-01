-- 실패 결제는 paid_at이 없어 표시 시 created_at(시드 삽입시각)으로 떨어진다.
-- 시도 시각이 보이도록 created_at을 실제 시도 시점으로 보정.
UPDATE payment SET created_at = '2026-06-20T03:10:00+09' WHERE id = 2;
