-- 과금 방식(generic): plan.pricing_type = flat(정액, 기본) | per_seat(인원당).
-- per_seat 이면 amount = 1인당 단가, 청구액 = 단가 × subscription.seats.
-- 특정 제품에 종속되지 않음 — 어느 제품이든 plan 에서 per_seat 를 고르면 인원 과금.

ALTER TABLE plan ADD COLUMN pricing_type VARCHAR(10) NOT NULL DEFAULT 'flat';
ALTER TABLE subscription ADD COLUMN seats INTEGER NOT NULL DEFAULT 1;

-- 데모: 문서분석 AI 에 인원당 과금 '팀' 플랜 추가(회사 단위 seat 과금 시연용)
INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, pricing_type,
                  features, is_highlight, sort_order)
VALUES (1, 'team', '팀', '인원당 과금 · 회사 단위', 8000, 'monthly', 'per_seat',
        '["인원당 월 8,000원","무제한 문서 분석","팀 공유 워크스페이스","관리자 콘솔"]'::jsonb, false, 4);
