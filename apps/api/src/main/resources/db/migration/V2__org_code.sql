-- 조직 테넌트 코드 — 관리자 승인 시 부여. 제품이 조직구독 그룹핑/프로비저닝에 사용하는 외부 키.
ALTER TABLE organization ADD COLUMN org_code VARCHAR(20);
CREATE UNIQUE INDEX uq_org_code ON organization (org_code) WHERE org_code IS NOT NULL;
COMMENT ON COLUMN organization.org_code IS '조직 테넌트 코드(SH-+대문자10, 예: SH-ABCDEFGHIJ) — 승인 시 부여, 사용자 노출, 제품이 조직구독 그룹핑에 사용';
