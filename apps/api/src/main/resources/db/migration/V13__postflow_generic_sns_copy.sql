-- 포스트플로우(service_code=post-flow)는 스레드만이 아니라 추후 다른 SNS도 지원 예정.
-- 플랜 기능 문구의 단일 플랫폼 표현 '스레드' → 범용 '생성' 으로 교체 + Pro 생성 한도 명시(월 200개).
--   basic: "월 50개 스레드" → "월 50개 생성"
--   pro:   "무제한 스레드" → "월 200개 생성"
-- (이미 범용인 free "총 10개 생성" 등은 불변. 신규 설치 seed(V1)와 동일한 최종 문구로 수렴.)
UPDATE plan SET features = '["월 50개 생성","1개 채널 연동","기본 템플릿"]'::jsonb
  WHERE product_id = (SELECT id FROM product WHERE service_code = 'post-flow') AND plan_code = 'basic';

UPDATE plan SET features = '["월 200개 생성","5개 채널 연동","예약 발행","성과 분석"]'::jsonb
  WHERE product_id = (SELECT id FROM product WHERE service_code = 'post-flow') AND plan_code = 'pro';
