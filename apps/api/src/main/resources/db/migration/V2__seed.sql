-- 데모 시드 (company_id = 1 단일 법인). 프론트 목업과 동일한 데이터.
-- 멀티테넌트 시드 원칙: company_id 있는 테이블은 명시 company_id로 시드.

-- 제품
INSERT INTO product (id, company_id, service_code, name, category, description, domain_url, demo_url, webhook_url, status, sort_order) VALUES
 (1, 1, 'doc-analysis', '문서분석 AI',     '생산성', '계약서·보고서를 업로드하면 핵심 조항과 리스크를 자동 요약·추출합니다.', 'https://docs.synub.io',    null, 'https://docs.synub.io/webhooks/billing',    'active', 1),
 (2, 1, 'threads',      '스레드 자동생성',  '마케팅', '키워드 하나로 SNS 스레드·게시물 초안을 자동 생성하고 예약 발행합니다.', 'https://threads.synub.io', null, 'https://threads.synub.io/webhooks/billing', 'active', 2),
 (3, 1, 'office',       '회계 자동화',     '재무',  '증빙 업로드부터 자동 분개·전표 생성, 부가세 신고 자료까지 한 번에.',     'https://office.synub.io',  null, 'https://office.synub.io/webhooks/billing',  'active', 3),
 (4, 1, 'desk',         '고객지원 데스크',  'CS',   '문의를 AI가 1차 분류·답변 추천하고, 상담 이력을 자동 정리합니다.',       'https://desk.synub.io',    null, 'https://desk.synub.io/webhooks/billing',    'active', 4);

-- 요금제
INSERT INTO plan (id, product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order) VALUES
 (1, 1, 'basic',      'Basic',      '개인·소규모 팀',        19000, 'monthly', '["월 200건 분석","기본 요약 템플릿","이메일 지원"]'::jsonb, false, 1),
 (2, 1, 'pro',        'Pro',        '성장하는 팀에 최적',     29000, 'monthly', '["무제한 문서 분석","리스크 하이라이트","API 액세스","우선 지원"]'::jsonb, true, 2),
 (3, 1, 'enterprise', 'Enterprise', '대규모 조직',           99000, 'monthly', '["전용 인스턴스","SSO·감사 로그","전담 매니저","SLA 보장"]'::jsonb, false, 3),
 (4, 2, 'basic',      'Basic',      '크리에이터 입문',        15000, 'monthly', '["월 50개 스레드","1개 채널 연동","기본 템플릿"]'::jsonb, false, 1),
 (5, 2, 'pro',        'Pro',        '전문 크리에이터',        25000, 'monthly', '["무제한 스레드","5개 채널 연동","예약 발행","성과 분석"]'::jsonb, true, 2),
 (6, 3, 'pro',        'Pro',        '법인·개인사업자',        39000, 'monthly', '["자동 분개","전표 관리","부가세 신고자료","회계사 공유"]'::jsonb, true, 1),
 (7, 3, 'yearly',     'Pro 연간',    '2개월 무료 (17% 할인)', 390000, 'yearly',  '["Pro 전체 기능","연간 일괄 결제","우선 지원"]'::jsonb, false, 2),
 (8, 4, 'basic',      'Basic',      '1~3인 CS팀',            22000, 'monthly', '["월 1,000 문의","AI 답변 추천","기본 리포트"]'::jsonb, true, 1);

-- 데모 고객
INSERT INTO customer (id, company_id, external_id, email) VALUES
 (1, 1, 'demo-user', 'deerkrg@synub.io');

-- 빌링키 (카드)
INSERT INTO billing_key (id, customer_id, pg_billing_key, card_company, card_last4, card_type, is_primary, status) VALUES
 (1, 1, 'billing_key_shinhan_demo', '신한카드', '4921', '신용', true,  'active'),
 (2, 1, 'billing_key_hyundai_demo', '현대카드', '1056', '체크', false, 'active');

-- 구독
INSERT INTO subscription (id, customer_id, plan_id, billing_key_id, status, started_at, next_billing_date, cancel_at_period_end, canceled_at) VALUES
 (1, 1, 2, 1, 'active',    '2026-01-12T00:00:00+09', '2026-07-12', false, null),
 (2, 1, 7, 1, 'active',    '2026-03-01T00:00:00+09', '2027-03-01', false, null),
 (3, 1, 5, 2, 'past_due',  '2026-02-20T00:00:00+09', '2026-06-20', false, null),
 (4, 1, 8, 2, 'canceled',  '2025-11-05T00:00:00+09', '2026-07-05', true,  '2026-06-15T10:00:00+09');

-- 결제 이력
INSERT INTO payment (id, subscription_id, pg_payment_id, amount, status, paid_at, receipt_no, failure_reason) VALUES
 (1, 1, 'pay_20260612_0001', 29000, 'paid',     '2026-06-12T09:30:00+09', '20260612-0001', null),
 (2, 3, null,                25000, 'failed',    null,                     null,            '카드 한도 초과'),
 (3, 4, 'pay_20260601_0007', 22000, 'refunded', '2026-06-01T14:05:00+09', '20260601-0007', null),
 (4, 1, 'pay_20260512_0001', 29000, 'paid',     '2026-05-12T09:30:00+09', '20260512-0001', null),
 (5, 2, 'pay_20260301_0042', 390000,'paid',     '2026-03-01T00:05:00+09', '20260301-0042', null),
 (6, 3, 'pay_20260420_0018', 25000, 'paid',     '2026-04-20T03:10:00+09', '20260420-0018', null);

-- IDENTITY 시퀀스를 시드 최대 id 다음으로 정렬 (이후 INSERT 충돌 방지)
SELECT setval(pg_get_serial_sequence('product', 'id'),      (SELECT max(id) FROM product));
SELECT setval(pg_get_serial_sequence('plan', 'id'),         (SELECT max(id) FROM plan));
SELECT setval(pg_get_serial_sequence('customer', 'id'),     (SELECT max(id) FROM customer));
SELECT setval(pg_get_serial_sequence('billing_key', 'id'),  (SELECT max(id) FROM billing_key));
SELECT setval(pg_get_serial_sequence('subscription', 'id'), (SELECT max(id) FROM subscription));
SELECT setval(pg_get_serial_sequence('payment', 'id'),      (SELECT max(id) FROM payment));
