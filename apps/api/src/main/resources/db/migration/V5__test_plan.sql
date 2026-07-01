-- 실연동(포트원) 테스트용 저가 플랜. 문서분석 AI에 1,000원 월간 플랜 추가.
-- 테스트 모드 결제는 실제 출금 후 그날 밤 자동취소되므로 소액으로 안전하게 검증.
INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order)
VALUES (1, 'test', '테스트', '실연동 결제 테스트용 (자동취소)', 1000, 'monthly',
        '["실결제 연동 점검 전용","1,000원 청구 후 야간 자동취소"]'::jsonb, false, 0);
