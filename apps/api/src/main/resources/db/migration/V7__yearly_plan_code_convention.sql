-- 연간 플랜 코드 규약 통일(<월간코드>_yearly) — 제품 페이지 월간/연간 토글이 같은 티어의 연간 플랜을
-- 찾아 연간 가격 표시·연간 구독하도록. (별도 '연간' 카드 대신 토글로 일원화)
-- Synub Works(product 3) Pro 연간: plan_code 'yearly' → 'pro_yearly'. Basic 연간은 이미 'basic_yearly'(V5).
UPDATE plan SET plan_code = 'pro_yearly'
  WHERE product_id = 3 AND plan_code = 'yearly' AND billing_cycle = 'yearly';
