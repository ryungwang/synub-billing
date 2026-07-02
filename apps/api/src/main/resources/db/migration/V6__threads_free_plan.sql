-- PostFlow(스레드 자동생성, service_code=threads, product_id=2)에 무료 체험 플랜 추가.
-- Free = 총 10개 생성(누적, 결제 없음 amount 0). 실제 사용량 게이팅은 제품(post-flow)이 담당.
-- 제품명·설명은 어드민 메타에서 관리하므로 여기선 플랜만 추가한다.

-- 시드가 명시 id로 삽입했으므로 IDENTITY 시퀀스를 현재 최대값으로 맞춘 뒤 자동 할당 삽입(id 충돌 방지).
SELECT setval(pg_get_serial_sequence('plan', 'id'), (SELECT MAX(id) FROM plan));

INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order) VALUES
 ((SELECT id FROM product WHERE service_code = 'threads'),
  'free', 'Free', '무료 체험', 0, 'monthly',
  '["총 10개 생성","기본 기능 체험"]'::jsonb, false, 0);
