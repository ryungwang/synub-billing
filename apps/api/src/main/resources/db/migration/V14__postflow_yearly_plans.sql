-- 포스트플로우(service_code=post-flow, product 2)에 연간 플랜 추가 — 지금까진 월간(Free/Basic/Pro)만 있어
-- 요금·제품 페이지의 월간/연간 토글을 연간으로 돌려도 대응 연간 플랜이 없어 월간 그대로였다.
-- 연간 코드 규약(<월간코드>_yearly, V7)·2개월 무료(월가×10, office V5) 규약 그대로 맞춘다.
--   Basic 15,000/월 → Basic 연간 150,000/년
--   Pro   25,000/월 → Pro 연간   250,000/년
-- 제품명·설명은 어드민 메타에서 관리하므로 여기선 플랜(가격·기능)만 추가한다.

-- 시드가 명시 id로 삽입했으므로 IDENTITY 시퀀스를 현재 최대값으로 맞춘 뒤 자동 할당 삽입(id 충돌 방지).
SELECT setval(pg_get_serial_sequence('plan', 'id'), (SELECT MAX(id) FROM plan));

INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order) VALUES
 ((SELECT id FROM product WHERE service_code = 'post-flow'),
  'basic_yearly', 'Basic 연간', '2개월 무료 (17% 할인)', 150000, 'yearly',
  '["Basic 전체 기능","연간 일괄 결제"]'::jsonb, false, 3),
 ((SELECT id FROM product WHERE service_code = 'post-flow'),
  'pro_yearly',   'Pro 연간',   '2개월 무료 (17% 할인)', 250000, 'yearly',
  '["Pro 전체 기능","연간 일괄 결제","우선 지원"]'::jsonb, false, 4);
