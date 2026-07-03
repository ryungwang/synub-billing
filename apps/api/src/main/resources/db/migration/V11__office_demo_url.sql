-- Synub Works(office) 데모 체험 URL — 제품 둘러보기의 "데모 체험하기" 링크 대상.
-- office 웹 /demo가 가입·SSO 없이 읽기전용 데모 세션을 자동 시작한다(APP_DEMO_ENABLED=true 필요).
UPDATE product SET demo_url = 'https://office.synub.io/demo' WHERE service_code = 'office';
