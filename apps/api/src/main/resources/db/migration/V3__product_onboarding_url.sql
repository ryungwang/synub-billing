-- 제품별 초기설정(온보딩) 페이지 URL — 셋업 필요 제품(그룹웨어 등)이 구독 후 서명 링크로 이동할 곳.
-- 범용: 어느 조직 제품이든 이 URL이 있으면 핸드오프 발급 가능.
ALTER TABLE product ADD COLUMN onboarding_url VARCHAR(255);
COMMENT ON COLUMN product.onboarding_url IS '제품 초기설정 온보딩 페이지 URL(예: https://office.synub.io/onboarding). 있으면 구독 후 핸드오프 링크 제공.';
