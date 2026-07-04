-- 연동 규칙 보정: 개발사 무상 구독을 제품 소유 스코프에 맞게 정리한다.
--   조직 전용 제품 → 개발사 org 소유 무상 구독(기존 유지)
--   개인 제품(org_only=false) → 개발자 개인(haru·sky) 소유 무상 구독(DeveloperSubscriptionSeeder 가 기동 시 생성)
-- 이 마이그레이션은 (1) 조직 제품 org_only 플래그를 바로잡고, (2) 개인 제품에 잘못 생성됐던
-- '조직 소유' 무상 구독을 제거해, 시더가 개인 소유로 재생성하도록 한다.

-- (1) 조직 전용 제품 플래그 보정(멱등) — 그룹웨어/Synub Works 는 회사 컨텍스트에서만 소비.
UPDATE product SET org_only = true WHERE service_code IN ('office', 'groupware');

-- (2) 개인 제품에 잘못 스코핑된 개발사 '조직 소유' 무상 구독 제거.
--     complimentary=true 이며 조직 소유인데 제품이 개인 제품(org_only=false)인 것만 대상(결제 구독·실고객 구독은 불변).
--     제거 후 다음 기동에서 시더가 개발자 개인(owner=customer) 소유로 재생성한다.
DELETE FROM subscription s
USING plan pl, product p
WHERE s.plan_id = pl.id
  AND pl.product_id = p.id
  AND s.complimentary = true
  AND s.owner_type = 'organization'
  AND p.org_only = false;
